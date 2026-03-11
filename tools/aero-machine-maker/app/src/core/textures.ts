import * as THREE from 'three'
import { getBlockInfo, blockRegistry } from './types'

const TILE_SIZE = 16
const ATLAS_TILES = 16

// Custom texture paths for specific mod blocks
const CUSTOM_TEXTURES: Record<string, string> = {
  controller: 'src/retronism/assets/block/retronism_crusher.png',
}

// Port textures by IO type (actual in-game textures)
const PORT_TEXTURES: Record<string, string> = {
  energy: 'src/retronism/assets/block/retronism_port_energy.png',
  fluid:  'src/retronism/assets/block/retronism_port_fluid.png',
  gas:    'src/retronism/assets/block/retronism_port_gas.png',
}

// Port overlay colors
const PORT_OVERLAYS: Record<string, string> = {
  energy_port: '#eeee00',
  fluid_port:  '#4488ff',
  gas_port:    '#bbbbbb',
  item_port:   '#ff8800',
}

let terrainCanvas: HTMLCanvasElement | null = null
const tileCache = new Map<string, HTMLCanvasElement>()
const customCache = new Map<string, HTMLCanvasElement>()
const portCache = new Map<string, HTMLCanvasElement>()
const materialCache = new Map<string, THREE.Material>()
let ready = false

function loadImageFromBase64(base64: string): Promise<HTMLCanvasElement | null> {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => {
      const c = document.createElement('canvas')
      c.width = img.width
      c.height = img.height
      const ctx = c.getContext('2d')!
      ctx.imageSmoothingEnabled = false
      ctx.drawImage(img, 0, 0)
      resolve(c)
    }
    img.onerror = () => resolve(null)
    img.src = `data:image/png;base64,${base64}`
  })
}

function extractTile(index: number): HTMLCanvasElement | null {
  const key = `tile_${index}`
  if (tileCache.has(key)) return tileCache.get(key)!
  if (!terrainCanvas) return null

  const col = index % ATLAS_TILES
  const row = Math.floor(index / ATLAS_TILES)
  const c = document.createElement('canvas')
  c.width = TILE_SIZE
  c.height = TILE_SIZE
  const ctx = c.getContext('2d')!
  ctx.imageSmoothingEnabled = false
  ctx.drawImage(terrainCanvas, col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE, 0, 0, TILE_SIZE, TILE_SIZE)
  tileCache.set(key, c)
  return c
}

function createPortTexture(baseTile: HTMLCanvasElement, overlayColor: string): HTMLCanvasElement {
  const cacheKey = `port_${overlayColor}`
  if (tileCache.has(cacheKey)) return tileCache.get(cacheKey)!

  const c = document.createElement('canvas')
  c.width = TILE_SIZE
  c.height = TILE_SIZE
  const ctx = c.getContext('2d')!
  ctx.imageSmoothingEnabled = false
  ctx.drawImage(baseTile, 0, 0)

  // Colored border (2px)
  ctx.fillStyle = overlayColor
  ctx.globalAlpha = 0.75
  ctx.fillRect(0, 0, TILE_SIZE, 2)
  ctx.fillRect(0, TILE_SIZE - 2, TILE_SIZE, 2)
  ctx.fillRect(0, 2, 2, TILE_SIZE - 4)
  ctx.fillRect(TILE_SIZE - 2, 2, 2, TILE_SIZE - 4)

  // Center diamond indicator
  ctx.globalAlpha = 0.9
  ctx.fillRect(7, 6, 2, 4)
  ctx.fillRect(6, 7, 4, 2)

  tileCache.set(cacheKey, c)
  return c
}

function canvasToTexture(canvas: HTMLCanvasElement): THREE.CanvasTexture {
  const tex = new THREE.CanvasTexture(canvas)
  tex.magFilter = THREE.NearestFilter
  tex.minFilter = THREE.NearestFilter
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

export async function loadTextures(): Promise<boolean> {
  const api = (window as any).api
  if (!api?.readFileBase64 || !api?.getProjectRoot) return false

  try {
    const root: string = await api.getProjectRoot()

    // Load terrain.png atlas
    const terrainB64 = await api.readFileBase64(`${root}/temp/merged/terrain.png`)
    if (terrainB64) {
      terrainCanvas = await loadImageFromBase64(terrainB64)
    }

    // Load custom block textures
    for (const [type, path] of Object.entries(CUSTOM_TEXTURES)) {
      const b64 = await api.readFileBase64(`${root}/${path}`)
      if (b64) {
        const canvas = await loadImageFromBase64(b64)
        if (canvas) customCache.set(type, canvas)
      }
    }

    // Load port textures (energy, fluid, gas)
    for (const [ioType, path] of Object.entries(PORT_TEXTURES)) {
      const b64 = await api.readFileBase64(`${root}/${path}`)
      if (b64) {
        const canvas = await loadImageFromBase64(b64)
        if (canvas) portCache.set(ioType, canvas)
      }
    }

    ready = !!terrainCanvas
    return ready
  } catch (e) {
    console.warn('Texture loading failed:', e)
    return false
  }
}

export function isTexturesReady(): boolean {
  return ready
}

/**
 * Extract a 16x16 tile from terrain.png as a data URL for use in UI (palette icons).
 * Returns null if terrain textures aren't loaded yet.
 */
export function getTileDataUrl(terrainIndex: number): string | null {
  if (!ready) return null
  const tile = extractTile(terrainIndex)
  if (!tile) return null
  return tile.toDataURL()
}

/**
 * Create a BoxGeometry with Minecraft-style per-face brightness baked into vertex colors.
 *
 * Minecraft Beta 1.7.3 face brightness multipliers (from RenderBlocks.java):
 *   Top    (Y+) = 1.0
 *   Bottom (Y-) = 0.5
 *   East   (X+) = 0.8
 *   West   (X-) = 0.8
 *   South  (Z+) = 0.6
 *   North  (Z-) = 0.6
 *
 * Three.js BoxGeometry face order: +X, -X, +Y, -Y, +Z, -Z
 * Each face has 4 vertices (indexed).
 */
let _mcGeom: THREE.BoxGeometry | null = null
export function getMinecraftBoxGeometry(): THREE.BoxGeometry {
  if (_mcGeom) return _mcGeom

  const geom = new THREE.BoxGeometry(0.92, 0.92, 0.92)

  // Minecraft face brightness: [+X, -X, +Y, -Y, +Z, -Z]
  const faceBrightness = [0.8, 0.8, 1.0, 0.5, 0.6, 0.6]

  const count = geom.attributes.position.count // 24 (4 vertices × 6 faces)
  const colors = new Float32Array(count * 3)

  for (let face = 0; face < 6; face++) {
    const b = faceBrightness[face]
    for (let v = 0; v < 4; v++) {
      const i = (face * 4 + v) * 3
      colors[i] = b
      colors[i + 1] = b
      colors[i + 2] = b
    }
  }

  geom.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  _mcGeom = geom
  return geom
}

/**
 * Get a block material using MeshBasicMaterial with vertexColors.
 * No dynamic lights needed — face brightness comes from vertex colors
 * (Minecraft-style per-face shading baked into the geometry).
 */
export function getBlockMaterial(type: string, portType?: string): THREE.Material {
  const key = `${type}_${portType || 'none'}`
  if (materialCache.has(key)) return materialCache.get(key)!

  const blockDef = blockRegistry.get(type)
  const isGlass = type === 'glass'
  let tileCanvas: HTMLCanvasElement | null = null

  if (ready) {
    // 1. Port texture: use actual in-game port texture based on portType
    if (portType && portCache.has(portType)) {
      tileCanvas = portCache.get(portType)!
    }
    // 2. Custom texture (controller, etc.)
    else if (customCache.has(type)) {
      tileCanvas = customCache.get(type)!
    }
    // 3. terrainIndex from block registry (all blocks with terrain textures)
    else if (blockDef?.terrainIndex !== undefined) {
      tileCanvas = extractTile(blockDef.terrainIndex)
    }
  }

  let mat: THREE.Material
  if (tileCanvas) {
    mat = new THREE.MeshBasicMaterial({
      map: canvasToTexture(tileCanvas),
      vertexColors: true,
      transparent: isGlass,
      opacity: isGlass ? 0.5 : 1.0,
    })
  } else {
    // Fallback: solid color (before textures load or if missing)
    mat = new THREE.MeshBasicMaterial({
      color: getBlockInfo(type).color,
      vertexColors: true,
      transparent: isGlass,
      opacity: isGlass ? 0.4 : 1.0,
    })
  }

  materialCache.set(key, mat)
  return mat
}

export function clearMaterialCache(): void {
  for (const mat of materialCache.values()) mat.dispose()
  materialCache.clear()
}
