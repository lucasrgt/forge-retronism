import { useEffect, useRef, useState, useCallback } from 'react'
import * as THREE from 'three'
import { OBJLoader } from 'three/addons/loaders/OBJLoader.js'
import { useStore, type AnimStateMapping } from '@/hooks/use-store'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

export function ModelEditor() {
  const canvasRef = useRef<HTMLDivElement>(null)
  const rendererRef = useRef<THREE.WebGLRenderer | null>(null)
  const sceneRef = useRef<THREE.Scene | null>(null)
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null)
  const modelRef = useRef<THREE.Group | null>(null)
  const animFrameRef = useRef<number>(0)

  const animConfig = useStore((s) => s.animConfig)

  // Camera orbit state
  const orbitRef = useRef({ theta: 45, phi: 30, radius: 3, target: new THREE.Vector3(0, 0.5, 0) })
  const draggingRef = useRef(false)
  const lastMouseRef = useRef({ x: 0, y: 0 })

  // Animation playback
  const [selectedClip, setSelectedClip] = useState<string | null>(null)
  const [playing, setPlaying] = useState(false)
  const playingRef = useRef(false)
  const clipTimeRef = useRef(0)
  const lastFrameRef = useRef(0)

  // Bone group refs for animation
  const boneGroupsRef = useRef<Map<string, THREE.Group>>(new Map())

  // -----------------------------------------------------------------------
  // Three.js setup
  // -----------------------------------------------------------------------
  useEffect(() => {
    const container = canvasRef.current
    if (!container) return

    const scene = new THREE.Scene()
    scene.background = new THREE.Color(0x1a1a2e)
    sceneRef.current = scene

    const camera = new THREE.PerspectiveCamera(50, container.clientWidth / container.clientHeight, 0.01, 100)
    cameraRef.current = camera

    const renderer = new THREE.WebGLRenderer({ antialias: true })
    renderer.setSize(container.clientWidth, container.clientHeight)
    renderer.setPixelRatio(window.devicePixelRatio)
    container.appendChild(renderer.domElement)
    rendererRef.current = renderer

    // Lighting
    const ambient = new THREE.AmbientLight(0xffffff, 0.6)
    scene.add(ambient)
    const directional = new THREE.DirectionalLight(0xffffff, 0.8)
    directional.position.set(3, 5, 3)
    scene.add(directional)

    // Grid helper (1 block = 1 unit)
    const grid = new THREE.GridHelper(4, 16, 0x444466, 0x333355)
    scene.add(grid)

    // Axis helper
    const axes = new THREE.AxesHelper(1)
    scene.add(axes)

    // Render loop
    function animate() {
      animFrameRef.current = requestAnimationFrame(animate)

      // Update camera from orbit
      const o = orbitRef.current
      const sinPhi = Math.sin(o.phi * Math.PI / 180)
      const cosPhi = Math.cos(o.phi * Math.PI / 180)
      const sinTheta = Math.sin(o.theta * Math.PI / 180)
      const cosTheta = Math.cos(o.theta * Math.PI / 180)
      camera.position.set(
        o.target.x + o.radius * cosPhi * sinTheta,
        o.target.y + o.radius * sinPhi,
        o.target.z + o.radius * cosPhi * cosTheta,
      )
      camera.lookAt(o.target)

      // Animation playback
      if (playingRef.current && selectedClip && animConfig.animJson) {
        const now = performance.now()
        const dt = (now - lastFrameRef.current) / 1000
        lastFrameRef.current = now

        const clipData = animConfig.animJson.animations?.[selectedClip]
        if (clipData) {
          clipTimeRef.current += dt
          if (clipData.loop) {
            clipTimeRef.current = clipTimeRef.current % clipData.length
          } else {
            clipTimeRef.current = Math.min(clipTimeRef.current, clipData.length)
          }
          applyAnimation(clipData, clipTimeRef.current)
        }
      }

      renderer.render(scene, camera)
    }
    animate()

    // Resize handler
    const onResize = () => {
      if (!container) return
      const w = container.clientWidth
      const h = container.clientHeight
      camera.aspect = w / h
      camera.updateProjectionMatrix()
      renderer.setSize(w, h)
    }
    const resizeObs = new ResizeObserver(onResize)
    resizeObs.observe(container)

    return () => {
      cancelAnimationFrame(animFrameRef.current)
      resizeObs.disconnect()
      renderer.dispose()
      container.removeChild(renderer.domElement)
    }
  }, [])

  // -----------------------------------------------------------------------
  // Apply animation keyframes to bone groups
  // -----------------------------------------------------------------------
  const applyAnimation = useCallback((clipData: any, time: number) => {
    const pivots = animConfig.animJson?.pivots || {}
    for (const [boneName, boneData] of Object.entries(clipData.bones as Record<string, any>)) {
      const group = boneGroupsRef.current.get(boneName)
      if (!group) continue

      // Rotation
      if (boneData.rotation) {
        const rot = sampleKeyframes(boneData.rotation, time)
        if (rot) {
          group.rotation.set(
            rot[0] * Math.PI / 180,
            rot[1] * Math.PI / 180,
            rot[2] * Math.PI / 180,
          )
        }
      }

      // Position
      if (boneData.position) {
        const pos = sampleKeyframes(boneData.position, time)
        if (pos) {
          const pivot = pivots[boneName] || [0, 0, 0]
          // Position is in Blockbench pixels, divide by 16 for block units
          group.position.set(
            pivot[0] / 16 + pos[0] / 16,
            pivot[1] / 16 + pos[1] / 16,
            pivot[2] / 16 + pos[2] / 16,
          )
        }
      }
    }
  }, [animConfig.animJson])

  // -----------------------------------------------------------------------
  // Load OBJ model when objContent changes
  // -----------------------------------------------------------------------
  useEffect(() => {
    const scene = sceneRef.current
    if (!scene) return

    // Remove old model
    if (modelRef.current) {
      scene.remove(modelRef.current)
      modelRef.current = null
      boneGroupsRef.current.clear()
    }

    if (!animConfig.objContent) return

    const loader = new OBJLoader()
    const obj = loader.parse(animConfig.objContent)

    // Apply default material
    const material = new THREE.MeshStandardMaterial({
      color: 0xcccccc,
      roughness: 0.7,
      metalness: 0.2,
      side: THREE.DoubleSide,
    })
    obj.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        child.material = material
      }
    })

    // Wrap named groups in Three.js Groups for animation
    // OBJLoader creates children for each "o" / "g" directive
    const wrapper = new THREE.Group()
    const pivots = animConfig.animJson?.pivots || {}

    // Collect children by name before reparenting
    const namedChildren: { name: string; child: THREE.Object3D }[] = []
    for (const child of [...obj.children]) {
      if (child.name) {
        namedChildren.push({ name: child.name, child })
      }
    }

    // Create bone groups with pivots
    for (const { name, child } of namedChildren) {
      const boneGroup = new THREE.Group()
      boneGroup.name = `bone_${name}`

      // Set pivot point
      const pivot = pivots[name]
      if (pivot) {
        boneGroup.position.set(pivot[0] / 16, pivot[1] / 16, pivot[2] / 16)
        // Offset child geometry back by pivot so rotation happens around the pivot
        child.position.set(-pivot[0] / 16, -pivot[1] / 16, -pivot[2] / 16)
      }

      obj.remove(child)
      boneGroup.add(child)
      wrapper.add(boneGroup)
      boneGroupsRef.current.set(name, boneGroup)
    }

    // Add remaining unnamed children directly
    for (const child of [...obj.children]) {
      obj.remove(child)
      wrapper.add(child)
    }

    // Scale: Blockbench exports in pixels (16px = 1 block). OBJ from BB is already in block units.
    // Center the model roughly
    const box = new THREE.Box3().setFromObject(wrapper)
    const center = box.getCenter(new THREE.Vector3())
    wrapper.position.sub(center)
    wrapper.position.y += box.getSize(new THREE.Vector3()).y / 2

    scene.add(wrapper)
    modelRef.current = wrapper
  }, [animConfig.objContent, animConfig.animJson])

  // -----------------------------------------------------------------------
  // Mouse controls (orbit)
  // -----------------------------------------------------------------------
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if (e.button === 0 && e.altKey) {
      draggingRef.current = true
      lastMouseRef.current = { x: e.clientX, y: e.clientY }
    }
    if (e.button === 1) { // middle click
      draggingRef.current = true
      lastMouseRef.current = { x: e.clientX, y: e.clientY }
    }
  }, [])

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!draggingRef.current) return
    const dx = e.clientX - lastMouseRef.current.x
    const dy = e.clientY - lastMouseRef.current.y
    lastMouseRef.current = { x: e.clientX, y: e.clientY }
    orbitRef.current.theta -= dx * 0.5
    orbitRef.current.phi = Math.max(-89, Math.min(89, orbitRef.current.phi + dy * 0.5))
  }, [])

  const handleMouseUp = useCallback(() => {
    draggingRef.current = false
  }, [])

  const handleWheel = useCallback((e: React.WheelEvent) => {
    orbitRef.current.radius = Math.max(0.5, Math.min(20, orbitRef.current.radius + e.deltaY * 0.005))
  }, [])

  // -----------------------------------------------------------------------
  // Playback controls
  // -----------------------------------------------------------------------
  const togglePlay = useCallback(() => {
    if (!selectedClip) return
    const next = !playing
    setPlaying(next)
    playingRef.current = next
    if (next) {
      clipTimeRef.current = 0
      lastFrameRef.current = performance.now()
    } else {
      // Reset bone transforms
      resetBoneTransforms()
    }
  }, [playing, selectedClip])

  const resetBoneTransforms = useCallback(() => {
    for (const [, group] of boneGroupsRef.current) {
      group.rotation.set(0, 0, 0)
      const pivots = animConfig.animJson?.pivots || {}
      const boneName = group.name.replace('bone_', '')
      const pivot = pivots[boneName]
      if (pivot) {
        group.position.set(pivot[0] / 16, pivot[1] / 16, pivot[2] / 16)
      } else {
        group.position.set(0, 0, 0)
      }
    }
  }, [animConfig.animJson])

  // -----------------------------------------------------------------------
  // File import handlers
  // -----------------------------------------------------------------------
  const handleImportObj = useCallback(async () => {
    const api = (window as any).api
    if (!api) return
    const filePath = await api.openDialog([{ name: 'OBJ Model', extensions: ['obj'] }])
    if (!filePath) return
    const content = await api.readFile(filePath)
    if (content) {
      useStore.getState().setAnimConfig({
        ...useStore.getState().animConfig,
        objPath: filePath,
        objContent: content,
      })
    }
  }, [])

  const handleImportBbmodel = useCallback(async () => {
    const api = (window as any).api
    if (!api) return
    const filePath = await api.openDialog([{ name: 'Blockbench Model', extensions: ['bbmodel'] }])
    if (!filePath) return
    const content = await api.readFile(filePath)
    if (content) {
      try {
        // Parse .bbmodel client-side (import the parser)
        const bbmodel = JSON.parse(content)
        const { parseBbmodel, toAeroAnimJson } = await import('./bbmodel-parser-client')
        const result = parseBbmodel(bbmodel)
        const animJson = toAeroAnimJson(result)

        useStore.getState().setAnimConfig({
          ...useStore.getState().animConfig,
          bbmodelPath: filePath,
          animJson,
          clipNames: result.clips.map((c: any) => c.name),
          boneNames: result.boneNames,
        })
      } catch (err) {
        console.error('Error parsing .bbmodel:', err)
      }
    }
  }, [])

  // -----------------------------------------------------------------------
  // State mapping UI
  // -----------------------------------------------------------------------
  const [newStateLabel, setNewStateLabel] = useState('')
  const [newStateClip, setNewStateClip] = useState('')

  const addStateMapping = useCallback(() => {
    if (!newStateLabel || !newStateClip) return
    const config = useStore.getState().animConfig
    const nextId = config.stateMappings.length === 0 ? 0 : Math.max(...config.stateMappings.map(m => m.stateId)) + 1
    useStore.getState().setAnimConfig({
      ...config,
      stateMappings: [...config.stateMappings, { stateId: nextId, label: newStateLabel, clipName: newStateClip }].sort((a, b) => a.stateId - b.stateId),
    })
    setNewStateLabel('')
    setNewStateClip('')
  }, [newStateLabel, newStateClip])

  const removeStateMapping = useCallback((stateId: number) => {
    const config = useStore.getState().animConfig
    useStore.getState().setAnimConfig({
      ...config,
      stateMappings: config.stateMappings.filter(m => m.stateId !== stateId),
    })
  }, [])

  // -----------------------------------------------------------------------
  // Render
  // -----------------------------------------------------------------------
  const hasObj = !!animConfig.objContent
  const hasAnims = animConfig.clipNames.length > 0

  return (
    <div className="flex flex-1 overflow-hidden">
      {/* 3D Viewport */}
      <div
        ref={canvasRef}
        className="flex-1 relative"
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onWheel={handleWheel}
      >
        {!hasObj && (
          <div className="absolute inset-0 flex items-center justify-center text-muted-foreground">
            <div className="text-center space-y-3">
              <p className="text-lg">No model loaded</p>
              <p className="text-sm">Import an OBJ file exported from Blockbench</p>
              <Button onClick={handleImportObj} variant="outline" size="sm">Import OBJ</Button>
            </div>
          </div>
        )}
      </div>

      {/* Right panel — animation config */}
      <div className="w-72 border-l border-border bg-card overflow-y-auto p-3 space-y-4">
        <h3 className="text-sm font-medium text-foreground">Model & Animation</h3>

        {/* Import buttons */}
        <div className="space-y-2">
          <Button onClick={handleImportObj} variant="outline" size="sm" className="w-full">
            {hasObj ? `OBJ: ${animConfig.objPath?.split(/[\\/]/).pop()}` : 'Import OBJ'}
          </Button>
          <Button onClick={handleImportBbmodel} variant="outline" size="sm" className="w-full">
            {hasAnims ? `BBModel: ${animConfig.bbmodelPath?.split(/[\\/]/).pop()}` : 'Import Animations (.bbmodel)'}
          </Button>
        </div>

        {/* Bones list */}
        {animConfig.boneNames.length > 0 && (
          <div>
            <h4 className="text-xs text-muted-foreground mb-1">Bones ({animConfig.boneNames.length})</h4>
            <div className="text-xs space-y-0.5">
              {animConfig.boneNames.map((name) => (
                <div key={name} className="px-2 py-0.5 bg-muted/50 rounded">{name}</div>
              ))}
            </div>
          </div>
        )}

        {/* Animation clips */}
        {hasAnims && (
          <div>
            <h4 className="text-xs text-muted-foreground mb-1">Animation Clips</h4>
            <div className="space-y-1">
              {animConfig.clipNames.map((clipName) => {
                const clipData = animConfig.animJson?.animations?.[clipName]
                const isSelected = selectedClip === clipName
                return (
                  <button
                    key={clipName}
                    className={`w-full text-left px-2 py-1 rounded text-xs ${isSelected ? 'bg-primary text-primary-foreground' : 'bg-muted/50 hover:bg-muted'}`}
                    onClick={() => {
                      setSelectedClip(clipName)
                      setPlaying(false)
                      playingRef.current = false
                      resetBoneTransforms()
                    }}
                  >
                    <span className="font-medium">{clipName}</span>
                    {clipData && (
                      <span className="ml-2 opacity-70">
                        {clipData.length}s {clipData.loop ? '🔁' : '▶'}
                      </span>
                    )}
                  </button>
                )
              })}
            </div>

            {/* Playback controls */}
            {selectedClip && (
              <div className="flex gap-2 mt-2">
                <Button onClick={togglePlay} variant="outline" size="sm" className="flex-1">
                  {playing ? '⏹ Stop' : '▶ Play'}
                </Button>
              </div>
            )}
          </div>
        )}

        {/* State mappings */}
        {hasAnims && (
          <div>
            <h4 className="text-xs text-muted-foreground mb-1">State Mappings</h4>
            <p className="text-xs text-muted-foreground mb-2">Map machine states to animation clips</p>

            {/* Existing mappings */}
            <div className="space-y-1 mb-2">
              {animConfig.stateMappings.map((m) => (
                <div key={m.stateId} className="flex items-center gap-1 text-xs">
                  <span className="bg-muted/50 px-1.5 py-0.5 rounded font-mono">{m.stateId}</span>
                  <span className="flex-1 truncate">{m.label}</span>
                  <span className="text-muted-foreground">→</span>
                  <span className="text-primary">{m.clipName}</span>
                  <button
                    className="text-destructive hover:text-destructive/80 px-1"
                    onClick={() => removeStateMapping(m.stateId)}
                  >×</button>
                </div>
              ))}
            </div>

            {/* Add new mapping */}
            <div className="flex gap-1">
              <Input
                placeholder="Label"
                value={newStateLabel}
                onChange={(e) => setNewStateLabel(e.target.value)}
                className="flex-1 h-7 text-xs"
              />
              <select
                value={newStateClip}
                onChange={(e) => setNewStateClip(e.target.value)}
                className="h-7 text-xs bg-background border border-border rounded px-1"
              >
                <option value="">Clip</option>
                {animConfig.clipNames.map((name) => (
                  <option key={name} value={name}>{name}</option>
                ))}
              </select>
              <Button onClick={addStateMapping} variant="outline" size="sm" className="h-7 px-2">+</Button>
            </div>
          </div>
        )}

        {/* Info */}
        {hasObj && (
          <div className="text-xs text-muted-foreground space-y-1">
            <div>Alt+drag: orbit | Scroll: zoom</div>
          </div>
        )}
      </div>
    </div>
  )
}

// -------------------------------------------------------------------------
// Keyframe sampling (linear interpolation, matches Aero_AnimationClip.sample)
// -------------------------------------------------------------------------
function sampleKeyframes(kfMap: Record<string, [number, number, number]>, time: number): [number, number, number] | null {
  const entries = Object.entries(kfMap)
    .map(([t, v]) => ({ time: parseFloat(t), value: v }))
    .sort((a, b) => a.time - b.time)

  if (entries.length === 0) return null
  if (entries.length === 1) return [...entries[0].value]
  if (time <= entries[0].time) return [...entries[0].value]
  if (time >= entries[entries.length - 1].time) return [...entries[entries.length - 1].value]

  // Find interval
  let lo = 0
  for (let i = 1; i < entries.length; i++) {
    if (entries[i].time >= time) { lo = i - 1; break }
  }
  const hi = lo + 1
  const t0 = entries[lo].time
  const t1 = entries[hi].time
  const alpha = t1 > t0 ? (time - t0) / (t1 - t0) : 0

  const a = entries[lo].value
  const b = entries[hi].value
  return [
    a[0] + (b[0] - a[0]) * alpha,
    a[1] + (b[1] - a[1]) * alpha,
    a[2] + (b[2] - a[2]) * alpha,
  ]
}
