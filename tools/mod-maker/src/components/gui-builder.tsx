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
}

type Color = readonly [number, number, number]

function rgb(c: Color) { return `rgb(${c[0]},${c[1]},${c[2]})` }

const PALETTE_ITEMS: { type: GuiComponentType; icon: string; cls: string }[] = [
  { type: 'slot', icon: 'Slot', cls: 'bg-[#8b8b8b] border border-[#373737]' },
  { type: 'big_slot', icon: 'Big Slot', cls: 'bg-[#8b8b8b] border-2 border-[#373737]' },
  { type: 'energy_bar', icon: 'Energy', cls: 'bg-gradient-to-t from-[#3bfb98] to-[#36e38a] border border-[#373737]' },
  { type: 'progress_arrow', icon: 'Arrow', cls: 'bg-[#c6c6c6]' },
  { type: 'flame', icon: 'Flame', cls: 'bg-gradient-to-t from-[#e44] to-[#fa0]' },
  { type: 'fluid_tank', icon: 'Fluid', cls: 'bg-[#48f] border border-[#373737]' },
  { type: 'gas_tank', icon: 'Gas', cls: 'bg-[#aaa] border border-[#373737]' },
  { type: 'separator', icon: 'Sep', cls: 'bg-[#555] !h-0.5 self-center' },
]

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
          if (comp.slotType === 'input') { ctx.fillStyle = '#4af'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText('IN', comp.x * s + 2, (comp.y - 4) * s) }
          if (comp.slotType === 'output') { ctx.fillStyle = '#f80'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText('OUT', comp.x * s + 2, (comp.y - 4) * s) }
          break
        case 'big_slot':
          drawSlot(comp.x, comp.y, 26, 26)
          ctx.fillStyle = '#f80'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText('OUT', comp.x * s + 2, (comp.y - 4) * s)
          break
        case 'energy_bar':
          drawSlot(comp.x, comp.y, comp.w, comp.h)
          for (let py = comp.y + 1 + Math.floor(comp.h * 0.3); py < comp.y + comp.h - 1; py++) {
            const c = py % 2 === 0 ? MC.ENERGY_A : MC.ENERGY_B
            hLine(comp.x + 1, py, comp.w - 2, c)
          }
          ctx.fillStyle = '#3fb'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText('NRG', comp.x * s, (comp.y - 4) * s)
          break
        case 'progress_arrow':
          fill(comp.x, comp.y + 3, 17, 11, MC.SL)
          for (let ai = 0; ai < 8; ai++) vLine(comp.x + 17 + ai, comp.y + 8 - ai, 1 + ai * 2, MC.SL)
          ctx.fillStyle = '#ccc'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText('ARROW', (comp.x + 4) * s, (comp.y - 4) * s)
          break
        case 'flame':
          fill(comp.x + 2, comp.y + 4, 10, 10, MC.SL)
          fill(comp.x + 4, comp.y + 1, 6, 3, MC.SL)
          ctx.fillStyle = '#fa0'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText('FIRE', comp.x * s, (comp.y - 4) * s)
          break
        case 'fluid_tank':
          drawSlot(comp.x, comp.y, comp.w, comp.h)
          for (let py = comp.y + 1 + Math.floor(comp.h * 0.2); py < comp.y + comp.h - 1; py++)
            hLine(comp.x + 1, py, comp.w - 2, MC.FLUID)
          ctx.fillStyle = '#48f'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText('FLUID', comp.x * s, (comp.y - 4) * s)
          break
        case 'gas_tank':
          drawSlot(comp.x, comp.y, comp.w, comp.h)
          for (let py = comp.y + 1 + Math.floor(comp.h * 0.2); py < comp.y + comp.h - 1; py++)
            hLine(comp.x + 1, py, comp.w - 2, MC.GAS)
          ctx.fillStyle = '#aaa'; ctx.font = `bold ${6 * s}px Consolas`; ctx.fillText('GAS', comp.x * s, (comp.y - 4) * s)
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
          {PALETTE_ITEMS.map((item) => (
            <button
              key={item.type}
              draggable
              onDragStart={(e) => { e.dataTransfer.setData('text/plain', item.type); e.dataTransfer.effectAllowed = 'copy' }}
              className="flex items-center gap-1.5 bg-secondary text-foreground border border-border px-2 py-1 rounded text-[11px] cursor-grab active:cursor-grabbing hover:border-blue-400 hover:bg-secondary/80 select-none"
            >
              <span className={`w-3.5 h-3.5 rounded-sm ${item.cls}`} />
              {item.icon}
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
