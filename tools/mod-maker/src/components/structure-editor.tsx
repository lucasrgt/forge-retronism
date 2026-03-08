import { useEffect, useRef, useState, useMemo, useCallback } from 'react'
import * as THREE from 'three'
import { useStore } from '@/hooks/use-store'
import { blockRegistry, type BlockEntry, type BlockCategory } from '@/core/types'
import { loadTextures, isTexturesReady, getBlockMaterial, clearMaterialCache, getTileDataUrl } from '@/core/textures'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import { BuildGuide } from '@/components/build-guide'

export function StructureEditor() {
  const containerRef = useRef<HTMLDivElement>(null)
  const sceneRef = useRef<{
    scene: THREE.Scene
    camera: THREE.PerspectiveCamera
    renderer: THREE.WebGLRenderer
    raycaster: THREE.Raycaster
    meshes: Map<string, THREE.Mesh>
    gridGroup: THREE.Group
    groundPlane: THREE.Mesh
    animId: number
    orbitState: { dragging: boolean; panning: boolean; lastX: number; lastY: number; theta: number; phi: number; radius: number }
    target: THREE.Vector3
    spaceDown: boolean
  } | null>(null)

  const blocks = useStore((s) => s.blocks)
  const dimensions = useStore((s) => s.dimensions)
  const selectedTool = useStore((s) => s.selectedTool)
  const selectedBlock = useStore((s) => s.selectedBlock)
  const layerFilter = useStore((s) => s.layerFilter)
  const pendingCamera = useStore((s) => s.pendingCamera)
  const pendingHighlight = useStore((s) => s.pendingHighlight)

  // Initialize Three.js
  useEffect(() => {
    const container = containerRef.current
    if (!container || sceneRef.current) return

    const scene = new THREE.Scene()
    scene.background = new THREE.Color(0x111111)

    const camera = new THREE.PerspectiveCamera(50, container.clientWidth / container.clientHeight, 0.1, 100)
    const renderer = new THREE.WebGLRenderer({ antialias: true })
    renderer.setSize(container.clientWidth, container.clientHeight)
    renderer.setPixelRatio(window.devicePixelRatio)
    container.appendChild(renderer.domElement)

    // Lights
    scene.add(new THREE.AmbientLight(0x404050, 1.5))
    const dirLight = new THREE.DirectionalLight(0xffffff, 1.0)
    dirLight.position.set(5, 8, 5)
    scene.add(dirLight)

    // Ground grid (populated by dimensions effect)
    const gridGroup = new THREE.Group()
    scene.add(gridGroup)

    const { w, h, d } = useStore.getState().dimensions
    const target = new THREE.Vector3(w / 2 - 0.5, h / 2 - 0.5, d / 2 - 0.5)
    const orbitState = { dragging: false, panning: false, lastX: 0, lastY: 0, theta: 0.6, phi: 0.8, radius: 8 }
    let spaceDown = false

    function updateCamera() {
      camera.position.set(
        target.x + orbitState.radius * Math.sin(orbitState.phi) * Math.cos(orbitState.theta),
        target.y + orbitState.radius * Math.cos(orbitState.phi),
        target.z + orbitState.radius * Math.sin(orbitState.phi) * Math.sin(orbitState.theta),
      )
      camera.lookAt(target)
    }
    updateCamera()

    // Keyboard: space for pan mode
    const canvas = renderer.domElement
    canvas.tabIndex = 0
    function onKeyDown(e: KeyboardEvent) {
      if (e.code === 'Space') { spaceDown = true; e.preventDefault() }
      if (e.code === 'Delete') {
        const s = useStore.getState()
        if (s.selectedBlock) {
          const [x, y, z] = s.selectedBlock.split(',').map(Number) as [number, number, number]
          s.removeBlock(x, y, z)
          s.setSelectedBlock(null)
        }
      }
    }
    function onKeyUp(e: KeyboardEvent) { if (e.code === 'Space') { spaceDown = false } }
    canvas.addEventListener('keydown', onKeyDown)
    canvas.addEventListener('keyup', onKeyUp)

    // Mouse controls: orbit (alt+drag / middle), pan (space+drag)
    canvas.addEventListener('mousedown', (e) => {
      if (e.altKey || e.button === 1) {
        orbitState.dragging = true
        orbitState.lastX = e.clientX
        orbitState.lastY = e.clientY
      } else if (spaceDown && e.button === 0) {
        orbitState.panning = true
        orbitState.lastX = e.clientX
        orbitState.lastY = e.clientY
      }
    })
    canvas.addEventListener('mousemove', (e) => {
      if (orbitState.panning) {
        // Pan: move target in camera-local XY plane
        const dx = (e.clientX - orbitState.lastX) * 0.002 * orbitState.radius
        const dy = (e.clientY - orbitState.lastY) * 0.002 * orbitState.radius
        const right = new THREE.Vector3()
        const up = new THREE.Vector3()
        camera.getWorldDirection(new THREE.Vector3())
        right.crossVectors(camera.up, new THREE.Vector3().subVectors(camera.position, target).normalize()).normalize()
        up.copy(camera.up)
        target.add(right.multiplyScalar(-dx))
        target.add(up.multiplyScalar(dy))
        orbitState.lastX = e.clientX
        orbitState.lastY = e.clientY
        updateCamera()
        return
      }
      if (!orbitState.dragging) return
      orbitState.theta += (e.clientX - orbitState.lastX) * 0.008
      orbitState.phi = Math.max(0.1, Math.min(Math.PI - 0.1, orbitState.phi - (e.clientY - orbitState.lastY) * 0.008))
      orbitState.lastX = e.clientX
      orbitState.lastY = e.clientY
      updateCamera()
    })
    canvas.addEventListener('mouseup', () => { orbitState.dragging = false; orbitState.panning = false })
    canvas.addEventListener('wheel', (e) => {
      orbitState.radius = Math.max(3, Math.min(30, orbitState.radius + e.deltaY * 0.01))
      updateCamera()
    })

    // Raycast helper
    const raycaster = new THREE.Raycaster()
    function raycastBlocks(e: MouseEvent) {
      const rect = canvas.getBoundingClientRect()
      const mouse = new THREE.Vector2(
        ((e.clientX - rect.left) / rect.width) * 2 - 1,
        -((e.clientY - rect.top) / rect.height) * 2 + 1,
      )
      raycaster.setFromCamera(mouse, camera)
      const meshArray = Array.from(sceneRef.current?.meshes.values() || [])
      return raycaster.intersectObjects(meshArray, false)
        .filter(hit => hit.object.userData.key)
    }

    // Invisible ground plane for placing blocks on empty space (y=0 layer)
    const groundPlane = new THREE.Mesh(
      new THREE.PlaneGeometry(20, 20),
      new THREE.MeshBasicMaterial({ visible: false, side: THREE.DoubleSide }),
    )
    groundPlane.rotation.x = -Math.PI / 2
    groundPlane.position.set(w / 2 - 0.5, -0.5, d / 2 - 0.5)
    groundPlane.userData.isGround = true
    scene.add(groundPlane)

    function raycastGround(e: MouseEvent) {
      const rect = canvas.getBoundingClientRect()
      const mouse = new THREE.Vector2(
        ((e.clientX - rect.left) / rect.width) * 2 - 1,
        -((e.clientY - rect.top) / rect.height) * 2 + 1,
      )
      raycaster.setFromCamera(mouse, camera)
      return raycaster.intersectObject(groundPlane)
    }

    // Left-click: select existing block, or place on face if clicking empty adjacent
    canvas.addEventListener('click', (e) => {
      if (e.altKey || spaceDown) return
      const hits = raycastBlocks(e)
      if (hits.length > 0) {
        const hit = hits[0]
        const key = hit.object.userData.key as string
        const store = useStore.getState()
        if (e.shiftKey) {
          // Shift+click: place block on adjacent face
          const normal = hit.face?.normal
          if (normal) {
            const pos = key.split(',').map(Number)
            const nx = pos[0] + Math.round(normal.x)
            const ny = pos[1] + Math.round(normal.y)
            const nz = pos[2] + Math.round(normal.z)
            const { w, h, d } = store.dimensions
            if (nx >= 0 && nx < w && ny >= 0 && ny < h && nz >= 0 && nz < d) {
              store.placeBlock(nx, ny, nz, store.selectedTool)
            }
          }
        } else {
          // Click: select block
          store.setSelectedBlock(key)
        }
      } else {
        // Click on empty space: place block on ground plane (y=0)
        const groundHits = raycastGround(e)
        if (groundHits.length > 0) {
          const point = groundHits[0].point
          const bx = Math.round(point.x)
          const bz = Math.round(point.z)
          const store = useStore.getState()
          const { w, h, d } = store.dimensions
          if (bx >= 0 && bx < w && bz >= 0 && bz < d) {
            store.placeBlock(bx, 0, bz, store.selectedTool)
          }
        }
      }
    })

    // Right-click: place block on adjacent face, or on ground if empty
    canvas.addEventListener('contextmenu', (e) => {
      e.preventDefault()
      if (e.altKey || spaceDown) return
      const hits = raycastBlocks(e)
      if (hits.length > 0) {
        const hit = hits[0]
        const normal = hit.face?.normal
        if (normal) {
          const store = useStore.getState()
          const pos = hit.object.userData.key.split(',').map(Number)
          const nx = pos[0] + Math.round(normal.x)
          const ny = pos[1] + Math.round(normal.y)
          const nz = pos[2] + Math.round(normal.z)
          const { w, h, d } = store.dimensions
          if (nx >= 0 && nx < w && ny >= 0 && ny < h && nz >= 0 && nz < d) {
            store.placeBlock(nx, ny, nz, store.selectedTool)
          }
        }
      } else {
        // Right-click on empty: place on ground
        const groundHits = raycastGround(e)
        if (groundHits.length > 0) {
          const point = groundHits[0].point
          const bx = Math.round(point.x)
          const bz = Math.round(point.z)
          const store = useStore.getState()
          const { w, h, d } = store.dimensions
          if (bx >= 0 && bx < w && bz >= 0 && bz < d) {
            store.placeBlock(bx, 0, bz, store.selectedTool)
          }
        }
      }
    })

    const animId = requestAnimationFrame(function animate() {
      sceneRef.current!.animId = requestAnimationFrame(animate)
      renderer.render(scene, camera)
    })

    sceneRef.current = { scene, camera, renderer, raycaster, meshes: new Map(), gridGroup, groundPlane, animId, orbitState, target, spaceDown }

    // Load real block textures from terrain.png + custom assets
    loadTextures().then((ok) => {
      if (ok) {
        clearMaterialCache()
        // Force re-render with textures by triggering a blocks update
        const s = useStore.getState()
        // Re-set blocks to trigger the sync effect
        useStore.setState({ blocks: new Map(s.blocks) })
      }
    })

    // Handle resize
    const resizeObs = new ResizeObserver(() => {
      if (!container) return
      camera.aspect = container.clientWidth / container.clientHeight
      camera.updateProjectionMatrix()
      renderer.setSize(container.clientWidth, container.clientHeight)
    })
    resizeObs.observe(container)

    return () => {
      cancelAnimationFrame(animId)
      resizeObs.disconnect()
      canvas.removeEventListener('keydown', onKeyDown)
      canvas.removeEventListener('keyup', onKeyUp)
      renderer.dispose()
      container.removeChild(renderer.domElement)
      sceneRef.current = null
    }
  }, [])

  // Port overlay: colored wireframe cube on blocks with portType
  const PORT_COLORS: Record<string, number> = { energy: 0xfbbf24, fluid: 0x3b82f6, gas: 0xa3a3a3, item: 0xf97316 }
  function updatePortOverlay(mesh: THREE.Mesh, portType: string | null) {
    const existing = mesh.children.find(c => c.userData.isPortOverlay)
    if (existing) mesh.remove(existing)
    if (!portType || !PORT_COLORS[portType]) return
    const edges = new THREE.EdgesGeometry(new THREE.BoxGeometry(1.0, 1.0, 1.0))
    const overlay = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: PORT_COLORS[portType], linewidth: 2 }))
    overlay.userData.isPortOverlay = true
    mesh.add(overlay)
  }

  // Controller overlay: red wireframe
  function updateControllerOverlay(mesh: THREE.Mesh, isController: boolean) {
    const existing = mesh.children.find(c => c.userData.isControllerOverlay)
    if (existing) mesh.remove(existing)
    if (!isController) return
    const edges = new THREE.EdgesGeometry(new THREE.BoxGeometry(1.05, 1.05, 1.05))
    const overlay = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: 0xff2222, linewidth: 3 }))
    overlay.userData.isControllerOverlay = true
    mesh.add(overlay)
  }

  // Sync blocks to meshes
  useEffect(() => {
    const s = sceneRef.current
    if (!s) return

    // Remove old meshes
    for (const [key, mesh] of s.meshes) {
      if (!blocks.has(key)) {
        s.scene.remove(mesh)
        mesh.geometry.dispose()
        ;(mesh.material as THREE.Material).dispose()
        s.meshes.delete(key)
      }
    }

    // Add/update
    for (const [key, block] of blocks) {
      const [x, y, z] = key.split(',').map(Number)

      // Layer filter
      if (layerFilter >= 0 && y !== layerFilter) {
        if (s.meshes.has(key)) {
          s.meshes.get(key)!.visible = false
        }
        continue
      }

      const isSelected = key === selectedBlock
      const portType = block.portType || null
      const blockDef = blockRegistry.get(block.type)
      const isCtrl = blockDef?.category === 'controller'
      let mesh = s.meshes.get(key)
      const needsNewMaterial = mesh && (
        mesh.userData.blockType !== block.type ||
        mesh.userData.selected !== isSelected
      )
      const needsOverlayUpdate = mesh && (
        mesh.userData.portType !== portType ||
        mesh.userData.isCtrl !== isCtrl
      )

      if (!mesh) {
        const geom = new THREE.BoxGeometry(0.92, 0.92, 0.92)
        const mat = getBlockMaterial(block.type, isSelected)
        mesh = new THREE.Mesh(geom, mat)
        mesh.position.set(x, y, z)
        mesh.userData.key = key
        mesh.userData.blockType = block.type
        mesh.userData.selected = isSelected
        mesh.userData.portType = portType
        mesh.userData.isCtrl = isCtrl
        s.scene.add(mesh)
        s.meshes.set(key, mesh)
        updatePortOverlay(mesh, portType)
        updateControllerOverlay(mesh, isCtrl)
      } else {
        mesh.visible = true
        if (needsNewMaterial) {
          mesh.material = getBlockMaterial(block.type, isSelected)
          mesh.userData.blockType = block.type
          mesh.userData.selected = isSelected
        }
        if (needsNewMaterial || needsOverlayUpdate) {
          mesh.userData.portType = portType
          mesh.userData.isCtrl = isCtrl
          updatePortOverlay(mesh, portType)
          updateControllerOverlay(mesh, isCtrl)
        }
      }
    }
  }, [blocks, selectedBlock, layerFilter])

  // Handle camera commands from MCP WebSocket
  useEffect(() => {
    if (!pendingCamera || !sceneRef.current) return
    const cmd = useStore.getState().consumeCameraCommand()
    if (!cmd) return
    const orbit = sceneRef.current.orbitState
    if (cmd.theta !== undefined) orbit.theta = cmd.theta
    if (cmd.phi !== undefined) orbit.phi = cmd.phi
    if (cmd.radius !== undefined) orbit.radius = cmd.radius
    // Trigger camera update by modifying position (render loop will pick it up)
    const { target } = sceneRef.current
    const camera = sceneRef.current.camera
    camera.position.set(
      target.x + orbit.radius * Math.sin(orbit.phi) * Math.cos(orbit.theta),
      target.y + orbit.radius * Math.cos(orbit.phi),
      target.z + orbit.radius * Math.sin(orbit.phi) * Math.sin(orbit.theta),
    )
    camera.lookAt(target)
  }, [pendingCamera])

  // Handle highlight commands from MCP WebSocket
  useEffect(() => {
    if (!pendingHighlight || !sceneRef.current) return
    const cmd = useStore.getState().consumeHighlightCommand()
    if (!cmd) return
    const { keys, duration } = cmd
    const meshes = sceneRef.current.meshes
    const originalMaterials: Map<string, any> = new Map()
    const highlightMat = new THREE.MeshStandardMaterial({ color: 0xffff00, emissive: 0xffff00, emissiveIntensity: 0.6 })

    // Apply highlight
    for (const key of keys) {
      const mesh = meshes.get(key)
      if (mesh) {
        originalMaterials.set(key, mesh.material)
        mesh.material = highlightMat
      }
    }

    // Revert after duration
    setTimeout(() => {
      for (const [key, mat] of originalMaterials) {
        const mesh = meshes.get(key)
        if (mesh) mesh.material = mat
      }
      highlightMat.dispose()
    }, duration)
  }, [pendingHighlight])

  // Update camera target, grid, and auto-fit when dimensions change
  useEffect(() => {
    if (sceneRef.current) {
      const { w, h, d } = dimensions
      sceneRef.current.target.set(w / 2 - 0.5, h / 2 - 0.5, d / 2 - 0.5)
      // Update ground plane position
      sceneRef.current.groundPlane.position.set(w / 2 - 0.5, -0.5, d / 2 - 0.5)
      // Auto-fit: radius based on the largest dimension
      const maxDim = Math.max(w, h, d)
      sceneRef.current.orbitState.radius = Math.max(5, maxDim * 2)

      // Rebuild ground grid
      const gg = sceneRef.current.gridGroup
      while (gg.children.length) gg.remove(gg.children[0])
      const gridW = Math.max(w, 5) + 4
      const gridD = Math.max(d, 5) + 4
      const mainMat = new THREE.LineBasicMaterial({ color: 0x333333 })
      for (let x = 0; x <= gridW; x++) {
        const gx = x - (gridW - w) / 2 - 0.5
        const geom = new THREE.BufferGeometry().setFromPoints([
          new THREE.Vector3(gx, -0.5, -(gridD - d) / 2 - 0.5),
          new THREE.Vector3(gx, -0.5, d + (gridD - d) / 2 - 0.5),
        ])
        gg.add(new THREE.Line(geom, mainMat))
      }
      for (let z = 0; z <= gridD; z++) {
        const gz = z - (gridD - d) / 2 - 0.5
        const geom = new THREE.BufferGeometry().setFromPoints([
          new THREE.Vector3(-(gridW - w) / 2 - 0.5, -0.5, gz),
          new THREE.Vector3(w + (gridW - w) / 2 - 0.5, -0.5, gz),
        ])
        gg.add(new THREE.Line(geom, mainMat))
      }
      // Build area border
      const areaMat = new THREE.LineBasicMaterial({ color: 0x666666 })
      const areaGeom = new THREE.BufferGeometry().setFromPoints([
        new THREE.Vector3(-0.5, -0.5, -0.5),
        new THREE.Vector3(w - 0.5, -0.5, -0.5),
        new THREE.Vector3(w - 0.5, -0.5, d - 0.5),
        new THREE.Vector3(-0.5, -0.5, d - 0.5),
        new THREE.Vector3(-0.5, -0.5, -0.5),
      ])
      gg.add(new THREE.Line(areaGeom, areaMat))
    }
  }, [dimensions])

  const [paletteOpen, setPaletteOpen] = useState(false)
  const [addBlockOpen, setAddBlockOpen] = useState(false)
  const registryVersion = useStore((s) => s.registryVersion)

  // Organize blocks by category (reactive to registryVersion)
  const mergedCategories = useMemo(() => {
    const cats: Record<string, (typeof blockRegistry extends Map<string, infer V> ? V : never)[]> = {}
    for (const def of blockRegistry.values()) {
      const cat = def.category
      if (!cats[cat]) cats[cat] = []
      cats[cat].push(def)
    }
    return cats
  }, [registryVersion])

  const categoryLabels: Record<string, string> = {
    controller: 'Controller',
    port: 'Port',
    mod: 'Mod',
    vanilla: 'Vanilla',
    custom: 'Custom',
  }

  const categoryOrder = ['controller', 'port', 'mod', 'vanilla', 'custom']

  const selectedDef = blockRegistry.get(selectedTool)

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* Compact toolbar: selected block + palette toggle + layer slider */}
      <div className="flex items-center justify-between px-3 py-1.5 bg-background border-b border-border gap-2">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setPaletteOpen(!paletteOpen)}
            className="flex items-center gap-2 px-2 py-1 rounded border border-border hover:bg-muted/50 transition-colors"
          >
            <BlockIcon def={selectedDef} size={20} />
            <span className="text-sm font-medium">{selectedDef?.label || selectedTool}</span>
            <span className="text-xs text-muted-foreground">{paletteOpen ? '\u25B2' : '\u25BC'}</span>
          </button>
        </div>
        <div className="flex items-center gap-3 text-xs">
          <label className="flex items-center gap-1.5">
            Layer:
            <input
              type="range"
              min={-1}
              max={dimensions.h - 1}
              value={layerFilter}
              onChange={(e) => useStore.getState().setLayerFilter(+e.target.value)}
              className="w-20"
            />
            <span className="text-foreground font-bold w-6">{layerFilter < 0 ? 'All' : layerFilter}</span>
          </label>
        </div>
      </div>

      {/* Block palette dropdown */}
      {paletteOpen && (
        <div className="bg-background border-b border-border px-3 py-2 max-h-72 overflow-auto">
          {categoryOrder.map((cat) => {
            const defs = mergedCategories[cat]
            if (!defs || defs.length === 0) return null
            return (
              <div key={cat} className="mb-2">
                <div className="text-[10px] text-muted-foreground font-semibold uppercase tracking-wider mb-1">
                  {categoryLabels[cat] || cat}
                </div>
                <div className="flex flex-wrap gap-1">
                  {defs.map((def) => (
                    <button
                      key={def.id}
                      title={`${def.label} (ID: ${def.mcId ?? '?'})`}
                      onClick={() => {
                        useStore.getState().setSelectedTool(def.id)
                        setPaletteOpen(false)
                      }}
                      className={`
                        flex items-center gap-1 px-1.5 py-0.5 rounded text-xs border transition-colors
                        ${selectedTool === def.id
                          ? 'border-primary bg-primary/20 text-primary-foreground'
                          : 'border-border hover:bg-muted/50 text-foreground'
                        }
                      `}
                    >
                      <BlockIcon def={def} size={16} />
                      <span className="max-w-[80px] truncate">{def.label}</span>
                    </button>
                  ))}
                </div>
              </div>
            )
          })}
          <div className="mt-2 pt-2 border-t border-border">
            <button
              onClick={() => setAddBlockOpen(!addBlockOpen)}
              className="flex items-center gap-1 px-2 py-1 rounded text-xs border border-dashed border-border hover:bg-muted/50 text-muted-foreground hover:text-foreground transition-colors"
            >
              <span className="text-sm font-bold">+</span> Add Block
            </button>
          </div>
          {addBlockOpen && <AddBlockForm onDone={() => setAddBlockOpen(false)} />}
        </div>
      )}

      {/* Three.js viewport */}
      <div ref={containerRef} className="flex-1 min-h-0 bg-[#111] cursor-crosshair" />

      {/* Build Guide collapsible */}
      <BuildGuideDropdown />
    </div>
  )
}

function BlockIcon({ def, size = 16 }: { def?: { color: number; terrainIndex?: number; id: string }; size?: number }) {
  if (!def) return <span className="inline-block rounded-sm border border-white/20" style={{ width: size, height: size, background: '#ff00ff' }} />

  // Try terrain texture icon
  const dataUrl = def.terrainIndex !== undefined ? getTileDataUrl(def.terrainIndex) : null
  if (dataUrl) {
    return <img src={dataUrl} width={size} height={size} className="rounded-sm border border-white/10" style={{ imageRendering: 'pixelated' }} />
  }

  // Fallback: color swatch
  return (
    <span
      className="inline-block rounded-sm border border-white/20"
      style={{ width: size, height: size, background: `#${def.color.toString(16).padStart(6, '0')}` }}
    />
  )
}


const CHAR_POOL = 'iklmnopqrstuvwxyzZ0123456789'
function nextAvailableChar(): string {
  const used = new Set([...blockRegistry.values()].map(b => b.char))
  for (const ch of CHAR_POOL) {
    if (!used.has(ch)) return ch
  }
  return '?'
}

function AddBlockForm({ onDone }: { onDone: () => void }) {
  const [id, setId] = useState('')
  const [label, setLabel] = useState('')
  const [category, setCategory] = useState<BlockCategory>('controller')
  const [color, setColor] = useState('#6a8a6a')
  const [mcId, setMcId] = useState(213)
  const registerBlock = useStore((s) => s.registerBlock)

  const handleSubmit = () => {
    const cleanId = id.toLowerCase().replace(/[^a-z0-9_]/g, '_').replace(/^_+/, '')
    if (!cleanId || blockRegistry.has(cleanId)) {
      toast.error(blockRegistry.has(cleanId) ? `Block "${cleanId}" already exists` : 'Invalid ID')
      return
    }
    const char = nextAvailableChar()
    registerBlock({
      id: cleanId,
      category,
      label: label || cleanId,
      color: parseInt(color.replace('#', ''), 16),
      char,
      builtIn: false,
      mcId: mcId || undefined,
      terrainIndex: 45,
    })
    toast.success(`Block "${cleanId}" registered`)
    setId('')
    setLabel('')
    onDone()
  }

  return (
    <div className="mt-2 p-2 rounded border border-border bg-muted/30 space-y-1.5">
      <div className="flex gap-1.5">
        <div className="flex-1">
          <Label>ID (snake_case)</Label>
          <Input value={id} onChange={(e) => setId(e.target.value)} placeholder="ozonizer_ctrl" />
        </div>
        <div className="flex-1">
          <Label>Label</Label>
          <Input value={label} onChange={(e) => setLabel(e.target.value)} placeholder="Ozonizer Controller" />
        </div>
      </div>
      <div className="flex gap-1.5">
        <div className="flex-1">
          <Label>Category</Label>
          <Select value={category} onChange={(e) => setCategory(e.target.value as BlockCategory)}>
            <option value="controller">Controller</option>
            <option value="mod">Mod</option>
            <option value="custom">Custom</option>
          </Select>
        </div>
        <div className="w-16">
          <Label>Color</Label>
          <input type="color" value={color} onChange={(e) => setColor(e.target.value)}
            className="w-full h-7 rounded border border-border cursor-pointer" />
        </div>
        <div className="w-16">
          <Label>MC ID</Label>
          <Input type="number" value={mcId} min={200} max={255} onChange={(e) => setMcId(+e.target.value)} />
        </div>
      </div>
      <div className="flex gap-1.5">
        <Button size="sm" onClick={handleSubmit}>Add</Button>
        <Button size="sm" variant="outline" onClick={onDone}>Cancel</Button>
      </div>
    </div>
  )
}

function BuildGuideDropdown() {
  const showBuildGuide = useStore((s) => s.showBuildGuide)

  return (
    <div className="border-t border-border bg-background">
      <button
        onClick={() => useStore.getState().setShowBuildGuide(!showBuildGuide)}
        className="w-full flex items-center justify-between px-3 py-1.5 text-xs text-muted-foreground hover:text-foreground hover:bg-muted/50 transition-colors"
      >
        <span className="font-semibold">Build Guide</span>
        <span>{showBuildGuide ? '\u25B2' : '\u25BC'}</span>
      </button>
      {showBuildGuide && (
        <div className="max-h-64 overflow-auto border-t border-border">
          <BuildGuide />
        </div>
      )}
    </div>
  )
}
