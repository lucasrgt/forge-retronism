import { useEffect, useRef, useCallback } from 'react'
import { useStore } from '@/hooks/use-store'
import { GUI_COMP_DEFS, type GuiComponentType, type GuiComponent } from '@/core/types'
import { Button } from '@/components/ui/button'
import { Select } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'
import { Card, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

const GUI_W = 176
const GUI_H = 166
const SCALE = 3

const MC = {
  BG: [198, 198, 198] as const,
  BK: [0, 0, 0] as const,
  WH: [255, 255, 255] as const,
  DK: [85, 85, 85] as const,
  SD: [55, 55, 55] as const,
  SL: [139, 139, 139] as const,
  ENERGY_A: [59, 251, 152] as const,
  ENERGY_B: [54, 227, 138] as const,
  FLUID: [40, 80, 220] as const,
  GAS: [170, 170, 170] as const,
  GAUGE: [86, 0, 1] as const,
}

type Color = readonly [number, number, number]

function rgb(c: Color) { return `rgb(${c[0]},${c[1]},${c[2]})` }

const PALETTE_ITEMS: GuiComponentType[] = [
  'slot', 'big_slot', 'energy_bar', 'progress_arrow', 'flame',
  'fluid_tank', 'gas_tank', 'fluid_tank_small', 'gas_tank_small', 'separator',
]

// Pixel-art icons for the palette toolbar (drawn on a tiny canvas)
const ICON_SCALE = 2 // each pixel = 2 CSS px
function PaletteIcon({ type }: { type: GuiComponentType }) {
  const ref = useRef<HTMLCanvasElement>(null)
  useEffect(() => {
    const c = ref.current; if (!c) return
    const ctx = c.getContext('2d')!
    const S = ICON_SCALE
    ctx.imageSmoothingEnabled = false
    ctx.clearRect(0, 0, c.width, c.height)

    const px = (x: number, y: number, col: string) => { ctx.fillStyle = col; ctx.fillRect(x * S, y * S, S, S) }
    const rect = (x: number, y: number, w: number, h: number, col: string) => { ctx.fillStyle = col; ctx.fillRect(x * S, y * S, w * S, h * S) }

    switch (type) {
      case 'slot': {
        // 9x9 slot: dark border top-left, white border bottom-right, gray fill
        rect(0, 0, 9, 9, '#8b8b8b')
        // top + left edge dark
        rect(0, 0, 9, 1, '#373737'); rect(0, 0, 1, 9, '#373737')
        // bottom + right edge white
        rect(0, 8, 9, 1, '#fff'); rect(8, 0, 1, 9, '#fff')
        break
      }
      case 'big_slot': {
        // 11x11 big slot
        rect(0, 0, 11, 11, '#8b8b8b')
        rect(0, 0, 11, 1, '#373737'); rect(0, 0, 1, 11, '#373737')
        rect(0, 10, 11, 1, '#fff'); rect(10, 0, 1, 11, '#fff')
        // inner border
        rect(1, 1, 9, 1, '#555'); rect(1, 1, 1, 9, '#555')
        break
      }
      case 'energy_bar': {
        // 5x11 energy bar with green gradient
        rect(0, 0, 5, 11, '#373737')
        rect(1, 1, 3, 9, '#555')
        // green fill from bottom
        for (let i = 0; i < 7; i++) {
          const g = 150 + Math.floor(105 * (1 - i / 7))
          rect(1, 3 + i, 3, 1, `rgb(59,${g},100)`)
        }
        break
      }
      case 'progress_arrow': {
        // 12x9 arrow — simplified furnace.png sprite
        const bg = '#2a2a3a' // transparent on dark bg
        rect(0, 0, 12, 9, bg)
        // shaft: rows 3-5, cols 0-6
        rect(0, 3, 7, 3, '#8b8b8b')
        // head: triangle from col 7, narrowing right
        for (let i = 0; i < 5; i++) {
          const h = 9 - i * 2
          const y = i
          rect(7 + i, y, 1, h, '#8b8b8b')
        }
        break
      }
      case 'flame': {
        // 7x7 flame — simplified furnace.png shape
        const flamePoints: [number, number][] = [
          [1,0], [5,0],
          [1,1], [3,1], [5,1],
          [1,2], [4,2], [5,2],
          [0,3], [1,3], [3,3], [4,3], [5,3], [6,3],
          [0,4], [1,4], [3,4], [5,4], [6,4],
          [0,5], [1,5], [3,5], [4,5], [5,5], [6,5],
          [0,6], [1,6], [2,6], [3,6], [4,6], [5,6], [6,6],
        ]
        for (const [x, y] of flamePoints) px(x, y, '#8b8b8b')
        break
      }
      case 'fluid_tank': {
        // 7x11 tank with blue gauge
        rect(0, 0, 7, 11, '#8b8b8b')
        rect(0, 0, 7, 1, '#373737'); rect(0, 0, 1, 11, '#373737')
        rect(0, 10, 7, 1, '#fff'); rect(6, 0, 1, 11, '#fff')
        // gauge strips bordeaux
        for (let row = 1; row < 10; row++) {
          const full = ((10 - row) % 5 === 0)
          const w = full ? 5 : 3
          const x = full ? 1 : 2
          rect(x, row, w, 1, '#560001')
        }
        break
      }
      case 'gas_tank': {
        // 7x11 tank with gray gauge
        rect(0, 0, 7, 11, '#8b8b8b')
        rect(0, 0, 7, 1, '#373737'); rect(0, 0, 1, 11, '#373737')
        rect(0, 10, 7, 1, '#fff'); rect(6, 0, 1, 11, '#fff')
        for (let row = 1; row < 10; row++) {
          const full = ((10 - row) % 5 === 0)
          const w = full ? 5 : 3
          const x = full ? 1 : 2
          rect(x, row, w, 1, '#560001')
        }
        break
      }
      case 'fluid_tank_small': {
        // 7x7 small tank blue
        rect(0, 0, 7, 7, '#8b8b8b')
        rect(0, 0, 7, 1, '#373737'); rect(0, 0, 1, 7, '#373737')
        rect(0, 6, 7, 1, '#fff'); rect(6, 0, 1, 7, '#fff')
        for (let row = 1; row < 6; row++) {
          const full = ((6 - row) % 5 === 0)
          rect(full ? 1 : 2, row, full ? 5 : 3, 1, '#560001')
        }
        break
      }
      case 'gas_tank_small': {
        // 7x7 small tank gray
        rect(0, 0, 7, 7, '#8b8b8b')
        rect(0, 0, 7, 1, '#373737'); rect(0, 0, 1, 7, '#373737')
        rect(0, 6, 7, 1, '#fff'); rect(6, 0, 1, 7, '#fff')
        for (let row = 1; row < 6; row++) {
          const full = ((6 - row) % 5 === 0)
          rect(full ? 1 : 2, row, full ? 5 : 3, 1, '#560001')
        }
        break
      }
      case 'separator': {
        // 11x3 separator line
        rect(0, 0, 11, 1, '#555')
        rect(0, 1, 11, 1, '#fff')
        break
      }
    }
  }, [type])

  const sizes: Record<GuiComponentType, [number, number]> = {
    slot: [9, 9], big_slot: [11, 11], energy_bar: [5, 11],
    progress_arrow: [12, 9], flame: [7, 7],
    fluid_tank: [7, 11], gas_tank: [7, 11],
    fluid_tank_small: [7, 7], gas_tank_small: [7, 7],
    separator: [11, 2],
  }
  const [w, h] = sizes[type]
  return <canvas ref={ref} width={w * ICON_SCALE} height={h * ICON_SCALE}
    style={{ width: w * ICON_SCALE, height: h * ICON_SCALE, imageRendering: 'pixelated' }} />
}

const PALETTE_LABELS: Record<GuiComponentType, string> = {
  slot: 'Slot', big_slot: 'Big Slot', energy_bar: 'Energy',
  progress_arrow: 'Arrow', flame: 'Flame',
  fluid_tank: 'Fluid', gas_tank: 'Gas',
  fluid_tank_small: 'Fluid S', gas_tank_small: 'Gas S',
  separator: 'Sep',
}

export function GuiBuilder() {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const dragging = useRef<{ index: number; offsetX: number; offsetY: number } | null>(null)

  const components = useStore((s) => s.guiComponents)
  const selectedCompIndex = useStore((s) => s.selectedCompIndex)
  const snapEnabled = useStore((s) => s.snapEnabled)
  const gridSize = useStore((s) => s.gridSize)

  // --- Drawing helpers ---
  const render = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')!
    ctx.imageSmoothingEnabled = false

    ctx.fillStyle = '#0a0a1a'
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    // Panel
    const s = SCALE
    const fill = (x: number, y: number, w: number, h: number, c: Color) => {
      ctx.fillStyle = rgb(c); ctx.fillRect(x * s, y * s, w * s, h * s)
    }
    const hLine = (x: number, y: number, len: number, c: Color) => fill(x, y, len, 1, c)
    const vLine = (x: number, y: number, len: number, c: Color) => fill(x, y, 1, len, c)
    const pixel = (x: number, y: number, c: Color) => fill(x, y, 1, 1, c)

    // Background
    fill(3, 3, GUI_W - 6, GUI_H - 6, MC.BG)
    // Borders
    hLine(2, 0, GUI_W - 4, MC.BK); hLine(2, GUI_H - 1, GUI_W - 4, MC.BK)
    vLine(0, 2, GUI_H - 4, MC.BK); vLine(GUI_W - 1, 2, GUI_H - 4, MC.BK)
    pixel(1, 1, MC.BK); pixel(GUI_W - 2, 1, MC.BK); pixel(1, GUI_H - 2, MC.BK); pixel(GUI_W - 2, GUI_H - 2, MC.BK)
    // Highlights
    hLine(2, 1, GUI_W - 4, MC.WH); hLine(2, 2, GUI_W - 4, MC.WH)
    vLine(1, 2, GUI_H - 4, MC.WH); vLine(2, 2, GUI_H - 4, MC.WH)
    // Shadows
    hLine(3, GUI_H - 2, GUI_W - 5, MC.DK); hLine(3, GUI_H - 3, GUI_W - 5, MC.DK)
    vLine(GUI_W - 2, 3, GUI_H - 5, MC.DK); vLine(GUI_W - 3, 3, GUI_H - 5, MC.DK)

    // Grid
    if (snapEnabled && gridSize > 1) {
      ctx.strokeStyle = 'rgba(255,255,255,0.08)'
      ctx.lineWidth = 1
      for (let x = gridSize; x < GUI_W; x += gridSize) {
        ctx.beginPath(); ctx.moveTo(x * s + 0.5, 3 * s); ctx.lineTo(x * s + 0.5, (GUI_H - 3) * s); ctx.stroke()
      }
      for (let y = gridSize; y < GUI_H; y += gridSize) {
        ctx.beginPath(); ctx.moveTo(3 * s, y * s + 0.5); ctx.lineTo((GUI_W - 3) * s, y * s + 0.5); ctx.stroke()
      }
    }

    // Tank gauge overlay: bordeaux strips (half-width normal, full-width every 5th)
    const drawTankGauge = (gx: number, gy: number, gw: number, gh: number) => {
      const totalLines = Math.floor(gh / 5) - 1
      if (totalLines < 1) return
      const halfW = Math.floor(gw / 2)
      for (let i = 1; i <= totalLines; i++) {
        const ly = gy + Math.floor(gh * i / (totalLines + 1))
        if (i % 5 === 0) {
          hLine(gx, ly, gw, MC.GAUGE)
        } else {
          hLine(gx, ly, halfW, MC.GAUGE)
        }
      }
    }

    // Player inventory
    const drawSlot = (sx: number, sy: number, sw: number, sh: number) => {
      hLine(sx, sy, sw - 1, MC.SD); vLine(sx, sy, sh - 1, MC.SD)
      hLine(sx, sy + sh - 1, sw, MC.WH); vLine(sx + sw - 1, sy, sh, MC.WH)
      fill(sx + 1, sy + 1, sw - 2, sh - 2, MC.SL); pixel(sx + sw - 1, sy, MC.SL)
    }
    for (let row = 0; row < 3; row++)
      for (let col = 0; col < 9; col++)
        drawSlot(7 + col * 18, 83 + row * 18, 18, 18)
    for (let col = 0; col < 9; col++)
      drawSlot(7 + col * 18, 141, 18, 18)

    // Labels
    ctx.fillStyle = '#555'
    ctx.font = `${8 * s}px Consolas, monospace`
    ctx.fillText('Title', 8 * s, 12 * s)
    ctx.fillText('Inventory', 8 * s, (GUI_H - 96 + 6) * s)

    // Components
    for (let i = 0; i < components.length; i++) {
      const comp = components[i]
      const selected = i === selectedCompIndex

      switch (comp.type) {
        case 'slot':
          drawSlot(comp.x, comp.y, 18, 18)
          { const modeLabel = comp.ioMode === 'input' ? 'IN' : comp.ioMode === 'output' ? 'OUT' : 'DSP'
            const modeColor = comp.ioMode === 'input' ? '#4af' : comp.ioMode === 'output' ? '#f80' : '#888'
            ctx.fillStyle = modeColor; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText(modeLabel, comp.x * s + 2, (comp.y - 4) * s) }
          break
        case 'big_slot':
          drawSlot(comp.x, comp.y, 26, 26)
          { const modeLabel = comp.ioMode === 'input' ? 'IN' : comp.ioMode === 'output' ? 'OUT' : 'DSP'
            const modeColor = comp.ioMode === 'input' ? '#4af' : comp.ioMode === 'output' ? '#f80' : '#888'
            ctx.fillStyle = modeColor; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText(modeLabel, comp.x * s + 2, (comp.y - 4) * s) }
          break
        case 'energy_bar':
          drawSlot(comp.x, comp.y, comp.w, comp.h)
          for (let py = comp.y + 1 + Math.floor(comp.h * 0.3); py < comp.y + comp.h - 1; py++) {
            const c = py % 2 === 0 ? MC.ENERGY_A : MC.ENERGY_B
            hLine(comp.x + 1, py, comp.w - 2, c)
          }
          { const ioLabel = comp.ioMode === 'input' ? 'NRG↓' : comp.ioMode === 'output' ? 'NRG↑' : 'NRG'
            ctx.fillStyle = '#3fb'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText(ioLabel, comp.x * s, (comp.y - 4) * s) }
          break
        case 'progress_arrow': {
          // Empty arrow (24x17) — pixel-exact from furnace.png (79,34)
          // 0x8b = SL (139), background = c6 (198)
          const ax = comp.x, ay = comp.y
          // Shaft: rows 7-9, cols 1-14
          fill(ax + 1, ay + 7, 14, 3, MC.SL)
          // Head triangle: col 15 starts at row 1, narrows 1px per row each side
          for (let i = 0; i < 8; i++) vLine(ax + 15 + i, ay + 1 + i, 15 - i * 2, MC.SL)
          ctx.fillStyle = '#ccc'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText('ARROW', (ax + 2) * s, (ay - 4) * s)
          break
        }
        case 'flame': {
          // Empty flame (14x14) — pixel-exact from furnace.png (56,36)
          // All non-background pixels are 0x8b = SL (139)
          const fx = comp.x, fy = comp.y
          const flamePixels: [number, number][] = [
            [2,1], [12,1],
            [2,2], [12,2],
            [3,3], [7,3], [11,3],
            [3,4], [7,4], [11,4],
            [2,5], [3,5], [8,5], [11,5], [12,5],
            [2,6], [3,6], [8,6], [11,6], [12,6],
            [1,7], [2,7], [3,7], [7,7], [8,7], [11,7], [12,7], [13,7],
            [1,8], [2,8], [7,8], [8,8], [12,8], [13,8],
            [1,9], [2,9], [6,9], [7,9], [8,9], [12,9], [13,9],
            [1,10], [2,10], [3,10], [6,10], [7,10], [11,10], [12,10], [13,10],
            [2,11], [3,11], [6,11], [7,11], [11,11], [12,11],
            [2,12], [3,12], [6,12], [7,12], [8,12], [11,12], [12,12],
            [1,13], [2,13], [3,13], [6,13], [7,13], [8,13], [11,13], [12,13], [13,13],
          ]
          for (const [dx, dy] of flamePixels) pixel(fx + dx, fy + dy, MC.SL)
          ctx.fillStyle = '#fa0'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText('FIRE', fx * s, (fy - 4) * s)
          break
        }
        case 'fluid_tank':
        case 'fluid_tank_small':
          drawSlot(comp.x, comp.y, comp.w, comp.h)
          drawTankGauge(comp.x + 1, comp.y + 1, comp.w - 2, comp.h - 2)
          { const ioLabel = comp.ioMode === 'input' ? 'FLD↓' : comp.ioMode === 'output' ? 'FLD↑' : comp.type === 'fluid_tank_small' ? 'FLD' : 'FLUID'
            ctx.fillStyle = '#48f'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText(ioLabel, comp.x * s, (comp.y - 4) * s) }
          break
        case 'gas_tank':
        case 'gas_tank_small':
          drawSlot(comp.x, comp.y, comp.w, comp.h)
          drawTankGauge(comp.x + 1, comp.y + 1, comp.w - 2, comp.h - 2)
          { const ioLabel = comp.ioMode === 'input' ? 'GAS↓' : comp.ioMode === 'output' ? 'GAS↑' : comp.type === 'gas_tank_small' ? 'GAS' : 'GAS'
            ctx.fillStyle = '#aaa'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText(ioLabel, comp.x * s, (comp.y - 4) * s) }
          break
        case 'separator':
          hLine(comp.x, comp.y, comp.w, MC.SD)
          hLine(comp.x, comp.y + 1, comp.w, MC.WH)
          break
      }

      if (selected) {
        ctx.strokeStyle = '#e94560'
        ctx.lineWidth = 2
        ctx.setLineDash([4, 2])
        ctx.strokeRect(comp.x * s - 1, comp.y * s - 1, comp.w * s + 2, comp.h * s + 2)
        ctx.setLineDash([])
      }
    }
  }, [components, selectedCompIndex, snapEnabled, gridSize])

  useEffect(() => { render() }, [render])

  // --- Canvas events ---
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    function toGui(e: MouseEvent) {
      const rect = canvas!.getBoundingClientRect()
      const x = (e.clientX - rect.left) / SCALE
      const y = (e.clientY - rect.top) / SCALE
      if (x < 0 || x >= GUI_W || y < 0 || y >= GUI_H) return null
      return { x, y }
    }

    function snapVal(v: number) {
      const s = useStore.getState()
      if (!s.snapEnabled || s.gridSize <= 1) return Math.round(v)
      return Math.round(v / s.gridSize) * s.gridSize
    }

    function onMouseDown(e: MouseEvent) {
      const pos = toGui(e)
      if (!pos) return
      const comps = useStore.getState().guiComponents
      for (let i = comps.length - 1; i >= 0; i--) {
        const c = comps[i]
        if (pos.x >= c.x && pos.x < c.x + c.w && pos.y >= c.y && pos.y < c.y + c.h) {
          useStore.getState().setSelectedComp(i)
          dragging.current = { index: i, offsetX: pos.x - c.x, offsetY: pos.y - c.y }
          return
        }
      }
      useStore.getState().setSelectedComp(-1)
    }

    function onMouseMove(e: MouseEvent) {
      if (!dragging.current) return
      const pos = toGui(e)
      if (!pos) return
      const comp = useStore.getState().guiComponents[dragging.current.index]
      if (!comp) return
      const nx = snapVal(pos.x - dragging.current.offsetX)
      const ny = snapVal(pos.y - dragging.current.offsetY)
      useStore.getState().updateGuiComponent(dragging.current.index, {
        x: Math.max(3, Math.min(GUI_W - comp.w - 3, nx)),
        y: Math.max(3, Math.min(GUI_H - comp.h - 3, ny)),
      })
    }

    function onMouseUp() { dragging.current = null }

    function onContextMenu(e: MouseEvent) {
      e.preventDefault()
      const pos = toGui(e)
      if (!pos) return
      const comps = useStore.getState().guiComponents
      for (let i = comps.length - 1; i >= 0; i--) {
        const c = comps[i]
        if (pos.x >= c.x && pos.x < c.x + c.w && pos.y >= c.y && pos.y < c.y + c.h) {
          useStore.getState().removeGuiComponent(i)
          return
        }
      }
    }

    function onDragOver(e: DragEvent) { e.preventDefault(); if (e.dataTransfer) e.dataTransfer.dropEffect = 'copy' }

    function onDrop(e: DragEvent) {
      e.preventDefault()
      const type = e.dataTransfer?.getData('text/plain') as GuiComponentType
      if (!type || !GUI_COMP_DEFS[type]) return
      const pos = toGui(e as any)
      if (!pos) return
      const def = GUI_COMP_DEFS[type]
      useStore.getState().addGuiComponent(type, Math.round(pos.x - def.w / 2), Math.round(pos.y - def.h / 2))
    }

    canvas.addEventListener('mousedown', onMouseDown)
    canvas.addEventListener('mousemove', onMouseMove)
    canvas.addEventListener('mouseup', onMouseUp)
    canvas.addEventListener('contextmenu', onContextMenu)
    canvas.addEventListener('dragover', onDragOver)
    canvas.addEventListener('drop', onDrop)

    return () => {
      canvas.removeEventListener('mousedown', onMouseDown)
      canvas.removeEventListener('mousemove', onMouseMove)
      canvas.removeEventListener('mouseup', onMouseUp)
      canvas.removeEventListener('contextmenu', onContextMenu)
      canvas.removeEventListener('dragover', onDragOver)
      canvas.removeEventListener('drop', onDrop)
    }
  }, [])

  const selectedComp = selectedCompIndex >= 0 ? components[selectedCompIndex] : null
  const selectedDef = selectedComp ? GUI_COMP_DEFS[selectedComp.type] : null

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* Toolbar */}
      <div className="flex items-center justify-between px-2 py-1.5 bg-card border-b border-border flex-wrap gap-1">
        <div className="flex gap-1 flex-wrap">
          {PALETTE_ITEMS.map((t) => (
            <button
              key={t}
              draggable
              onDragStart={(e) => { e.dataTransfer.setData('text/plain', t); e.dataTransfer.effectAllowed = 'copy' }}
              className="flex items-center gap-1.5 bg-secondary text-foreground border border-border px-2 py-1 rounded text-[11px] cursor-grab active:cursor-grabbing hover:border-blue-400 hover:bg-secondary/80 select-none"
            >
              <PaletteIcon type={t} />
              {PALETTE_LABELS[t]}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-1.5">
          <Button variant="outline" size="sm" onClick={() => {
            const preset = (document.getElementById('gui-preset-sel') as HTMLSelectElement)?.value || 'processor'
            useStore.getState().loadGuiPreset(preset)
          }}>Load Preset</Button>
          <Select id="gui-preset-sel" className="w-28">
            <option value="processor">Processor</option>
            <option value="dual_input">Dual Input</option>
            <option value="single_slot">Single Slot</option>
            <option value="tank">Tank</option>
          </Select>
          <Checkbox label="Snap" checked={snapEnabled} onChange={(e) => useStore.getState().setSnapEnabled((e.target as HTMLInputElement).checked)} />
          <Select className="w-16" value={gridSize} onChange={(e) => useStore.getState().setGridSize(+e.target.value)}>
            <option value={2}>2px</option>
            <option value={4}>4px</option>
            <option value={9}>9px</option>
            <option value={18}>18px</option>
          </Select>
          <Button variant="destructive" size="sm" onClick={() => useStore.getState().clearGui()}>Clear</Button>
        </div>
      </div>

      {/* Canvas + info panel */}
      <div className="flex-1 flex overflow-hidden bg-[#0a0a1a]">
        <div className="flex-1 flex items-center justify-center">
          <canvas
            ref={canvasRef}
            width={GUI_W * SCALE}
            height={GUI_H * SCALE}
            style={{ width: GUI_W * SCALE, height: GUI_H * SCALE, imageRendering: 'pixelated' }}
            className="border border-border cursor-crosshair"
          />
        </div>

        {selectedComp && selectedDef && (
          <Card className="w-48 m-2 shrink-0 h-fit">
            <CardTitle>Component</CardTitle>
            <p className="text-xs">Type: <span className="font-bold text-foreground">{selectedDef.label}</span></p>
            <div className="flex gap-1.5">
              <div><Label>X</Label><Input type="number" value={selectedComp.x} min={0} max={175}
                onChange={(e) => useStore.getState().updateGuiComponent(selectedCompIndex, { x: +e.target.value })} /></div>
              <div><Label>Y</Label><Input type="number" value={selectedComp.y} min={0} max={165}
                onChange={(e) => useStore.getState().updateGuiComponent(selectedCompIndex, { y: +e.target.value })} /></div>
            </div>
            {selectedDef.resizable && (
              <div className="flex gap-1.5">
                <div><Label>W</Label><Input type="number" value={selectedComp.w} min={1}
                  onChange={(e) => useStore.getState().updateGuiComponent(selectedCompIndex, { w: +e.target.value })} /></div>
                <div><Label>H</Label><Input type="number" value={selectedComp.h} min={1}
                  onChange={(e) => useStore.getState().updateGuiComponent(selectedCompIndex, { h: +e.target.value })} /></div>
              </div>
            )}
            {(selectedComp.type === 'slot' || selectedComp.type === 'big_slot') && (
              <>
                <Label>Slot Role</Label>
                <Select value={selectedComp.slotType || 'input'}
                  onChange={(e) => useStore.getState().updateGuiComponent(selectedCompIndex, { slotType: e.target.value as any })}>
                  <option value="input">Input</option>
                  <option value="output">Output</option>
                  <option value="fuel">Fuel</option>
                </Select>
              </>
            )}
            {!['progress_arrow', 'flame', 'separator'].includes(selectedComp.type) && (
              <>
                <Label>I/O Mode</Label>
                <Select value={selectedComp.ioMode}
                  onChange={(e) => useStore.getState().updateGuiComponent(selectedCompIndex, { ioMode: e.target.value as any })}>
                  <option value="input">Input</option>
                  <option value="output">Output</option>
                  <option value="display">Display Only</option>
                </Select>
              </>
            )}
            <Button variant="destructive" size="sm" className="w-full"
              onClick={() => useStore.getState().removeGuiComponent(selectedCompIndex)}>
              Delete
            </Button>
          </Card>
        )}
      </div>
    </div>
  )
}
