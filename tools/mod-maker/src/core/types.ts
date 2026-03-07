export const BLOCK_TYPES = {
  casing:      { color: 0x888888, label: 'Casing',      char: 'C' },
  controller:  { color: 0xee4444, label: 'Controller',  char: 'K' },
  energy_port: { color: 0xeeee00, label: 'Energy Port', char: 'E' },
  fluid_port:  { color: 0x4488ff, label: 'Fluid Port',  char: 'F' },
  gas_port:    { color: 0xaaaaaa, label: 'Gas Port',    char: 'G' },
  item_port:   { color: 0xff8800, label: 'Item Port',   char: 'I' },
  glass:       { color: 0x88ddff, label: 'Glass',       char: 'W' },
} as const

export type BlockType = keyof typeof BLOCK_TYPES
export type StructureType = 'machine' | 'tank' | 'reactor' | 'custom'
export type PortMode = 'input' | 'output' | 'input_output'
export type IOType = 'energy' | 'fluid' | 'gas' | 'item'
export type SlotRole = 'input' | 'output' | 'fuel'
export type IoMode = 'input' | 'output' | 'display'
export type GuiComponentType = 'slot' | 'big_slot' | 'energy_bar' | 'progress_arrow' | 'flame' | 'fluid_tank' | 'gas_tank' | 'separator'
export type BlockCategory = 'casing' | 'controller' | 'port' | 'glass' | 'custom'

export interface BlockDef {
  id: string
  category: BlockCategory
  label: string
  color: number
  char: string
  portType?: IOType
  builtIn: boolean
}

export interface BlockEntry {
  type: string  // BlockType or custom block id
  mode: PortMode
}

// Dynamic registry: starts with built-in blocks, extended by MCP custom blocks
export const blockRegistry = new Map<string, BlockDef>()
for (const [id, info] of Object.entries(BLOCK_TYPES)) {
  const category: BlockCategory = id === 'controller' ? 'controller' : id.endsWith('_port') ? 'port' : id === 'glass' ? 'glass' : 'casing'
  blockRegistry.set(id, {
    id,
    category,
    label: info.label,
    color: info.color,
    char: info.char,
    portType: id.endsWith('_port') ? id.replace('_port', '') as IOType : undefined,
    builtIn: true,
  })
}

export function getBlockInfo(type: string): { color: number; label: string; char: string } {
  const reg = blockRegistry.get(type)
  if (reg) return reg
  const builtin = (BLOCK_TYPES as Record<string, { color: number; label: string; char: string }>)[type]
  if (builtin) return builtin
  return { color: 0xff00ff, label: type, char: '?' }
}

export interface GuiComponent {
  type: GuiComponentType
  x: number
  y: number
  w: number
  h: number
  slotType: SlotRole | null
  ioMode: IoMode
}

export const GUI_COMP_DEFS: Record<GuiComponentType, { w: number; h: number; label: string; resizable: boolean; slotType?: SlotRole; ioMode: IoMode }> = {
  slot:           { w: 18, h: 18, label: 'Slot', resizable: false, slotType: 'input', ioMode: 'input' },
  big_slot:       { w: 26, h: 26, label: 'Big Slot', resizable: false, slotType: 'output', ioMode: 'output' },
  energy_bar:     { w: 8,  h: 54, label: 'Energy Bar', resizable: true, ioMode: 'display' },
  progress_arrow: { w: 24, h: 17, label: 'Progress Arrow', resizable: false, ioMode: 'display' },
  flame:          { w: 14, h: 14, label: 'Flame', resizable: false, ioMode: 'display' },
  fluid_tank:     { w: 18, h: 54, label: 'Fluid Tank', resizable: true, ioMode: 'input' },
  gas_tank:       { w: 18, h: 54, label: 'Gas Tank', resizable: true, ioMode: 'input' },
  separator:      { w: 162, h: 2, label: 'Separator', resizable: true, ioMode: 'display' },
}
