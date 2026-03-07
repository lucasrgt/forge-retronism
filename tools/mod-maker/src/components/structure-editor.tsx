import { useEffect, useRef } from 'react'
import * as THREE from 'three'
import { useStore } from '@/hooks/use-store'
import { blockRegistry } from '@/core/types'
import { loadTextures, isTexturesReady, getBlockMaterial, clearMaterialCache } from '@/core/textures'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'

export function StructureEditor() {
  const containerRef = useRef<HTMLDivElement>(null)
  const sceneRef = useRef<{
    scene: THREE.Scene
    camera: THREE.PerspectiveCamera
    renderer: THREE.WebGLRenderer
    raycaster: THREE.Raycaster
    meshes: Map<string, THREE.Mesh>
    gridGroup: THREE.Group
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
    function onKeyDown(e: KeyboardEvent) { if (e.code === 'Space') { spaceDown = true; e.preventDefault() } }
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
      return raycaster.intersectObjects(meshArray)
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
      }
    })

    // Right-click: place block on adjacent face
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
      }
    })

    const animId = requestAnimationFrame(function animate() {
      sceneRef.current!.animId = requestAnimationFrame(animate)
      renderer.render(scene, camera)
    })

    sceneRef.current = { scene, camera, renderer, raycaster, meshes: new Map(), gridGroup, animId, orbitState, target, spaceDown }

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
      let mesh = s.meshes.get(key)
      const needsNewMaterial = mesh && (
        mesh.userData.blockType !== block.type ||
        mesh.userData.selected !== isSelected
      )

      if (!mesh) {
        const geom = new THREE.BoxGeometry(0.92, 0.92, 0.92)
        const mat = getBlockMaterial(block.type, isSelected)
        mesh = new THREE.Mesh(geom, mat)
        mesh.position.set(x, y, z)
        mesh.userData.key = key
        mesh.userData.blockType = block.type
        mesh.userData.selected = isSelected
        s.scene.add(mesh)
        s.meshes.set(key, mesh)
      } else {
        mesh.visible = true
        if (needsNewMaterial) {
          const oldMat = mesh.material as THREE.Material
          mesh.material = getBlockMaterial(block.type, isSelected)
          mesh.userData.blockType = block.type
          mesh.userData.selected = isSelected
          // Don't dispose cached materials
        }
      }
    }
  }, [blocks, selectedBlock, layerFilter])

  // Update camera target, grid, and auto-fit when dimensions change
  useEffect(() => {
    if (sceneRef.current) {
      const { w, h, d } = dimensions
      sceneRef.current.target.set(w / 2 - 0.5, h / 2 - 0.5, d / 2 - 0.5)
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

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* Block palette toolbar */}
      <div className="flex items-center justify-between px-3 py-2 bg-background border-b border-border flex-wrap gap-1">
        <div className="flex gap-1 flex-wrap">
          {[...blockRegistry.values()].map((def) => (
            <Button
              key={def.id}
              variant={selectedTool === def.id ? 'default' : 'outline'}
              size="sm"
              onClick={() => useStore.getState().setSelectedTool(def.id)}
              className="gap-1"
            >
              <span
                className="w-3 h-3 rounded-sm border border-white/20"
                style={{ background: `#${def.color.toString(16).padStart(6, '0')}` }}
              />
              {def.label}
            </Button>
          ))}
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

      {/* Three.js viewport */}
      <div ref={containerRef} className="flex-1 bg-[#111] cursor-crosshair" />
    </div>
  )
}
