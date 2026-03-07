import * as THREE from 'three'
import { getBlockInfo } from './types'

const TILE_SIZE = 16
const ATLAS_TILES = 16

// Terrain.png tile index mapping for each block type
// Index = row * 16 + col in the 16x16 terrain atlas
const BLOCK_TEXTURE_DEFS: Record<string, {
  terrain?: number
  custom?: string    // relative path from project root
  overlay?: string   // hex color for port indicator
}> = {
  casing:      { terrain: 22 },  // iron block
  controller:  { custom: 'src/retronism/assets/block/retronism_crusher.png' },
  energy_port: { terrain: 22, overlay: '#eeee00' },
  fluid_port:  { terrain: 22, overlay: '#4488ff' },
  gas_port:    { terrain: 22, overlay: '#bbbbbb' },
  item_port:   { terrain: 22, overlay: '#ff8800' },
  glass:       { terrain: 49 },  // glass
}

let terrainCanvas: HTMLCanvasElement | null = null
const tileCache = new Map<string, HTMLCanvasElement>()
const customCache = new Map<string, HTMLCanvasElement>()
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
    for (const [type, def] of Object.entries(BLOCK_TEXTURE_DEFS)) {
      if (def.custom) {
        const b64 = await api.readFileBase64(`${root}/${def.custom}`)
        if (b64) {
          const canvas = await loadImageFromBase64(b64)
          if (canvas) customCache.set(type, canvas)
        }
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

export function getBlockMaterial(type: string, selected = false): THREE.Material {
  const key = `${type}_${selected ? 'sel' : 'n'}`
  if (materialCache.has(key)) return materialCache.get(key)!

  const def = BLOCK_TEXTURE_DEFS[type]
  const isGlass = type === 'glass'
  let tileCanvas: HTMLCanvasElement | null = null

  if (ready && def) {
    if (def.custom && customCache.has(type)) {
      tileCanvas = customCache.get(type)!
    } else if (def.terrain !== undefined) {
      tileCanvas = extractTile(def.terrain)
    }

    if (tileCanvas && def.overlay) {
      tileCanvas = createPortTexture(tileCanvas, def.overlay)
    }
  }

  let mat: THREE.Material
  if (tileCanvas) {
    mat = new THREE.MeshLambertMaterial({
      map: canvasToTexture(tileCanvas),
      transparent: isGlass,
      opacity: isGlass ? 0.5 : 1.0,
      emissive: selected ? 0x444444 : 0x000000,
    })
  } else {
    // Fallback: solid color (before textures load or if missing)
    mat = new THREE.MeshLambertMaterial({
      color: getBlockInfo(type).color,
      transparent: isGlass,
      opacity: isGlass ? 0.4 : 1.0,
      emissive: selected ? 0x444444 : 0x000000,
    })
  }

  materialCache.set(key, mat)
  return mat
}

export function clearMaterialCache(): void {
  for (const mat of materialCache.values()) mat.dispose()
  materialCache.clear()
}
