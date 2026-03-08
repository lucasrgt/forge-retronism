import { create } from 'zustand'
import type { BlockType, StructureType, IOType, PortMode, BlockEntry, GuiComponent, GuiComponentType, SlotRole, IoMode, BlockDef, BlockCategory } from '@/core/types'
import { BLOCK_TYPES, GUI_COMP_DEFS, blockRegistry, getBlockInfo } from '@/core/types'

const GUI_W = 176
const GUI_H = 166

// Serialized JSON format (matches MCP server output)
export interface SerializedMultiblock {
  name: string
  structType: StructureType
  dimensions: [number, number, number]
  ioTypes: IOType[]
  capacity: { energy: number; fluid: number; gas: number }
  processTime: number
  energyPerTick: number
  blockId: number
  casingId: number
  structure: { layer: number; pattern: string[][] }[]
  legend: Record<string, string>
  portModes: Record<string, PortMode>
  guiComponents: GuiComponent[]
  model?: { name: string; elements: { name: string; from: [number, number, number]; to: [number, number, number] }[]; textureName: string }
  registry?: BlockDef[]
}

interface MultiblockStore {
  // Structure config
  name: string
  structType: StructureType
  dimensions: { w: number; h: number; d: number }
  ioTypes: IOType[]
  capacity: { energy: number; fluid: number; gas: number }
  processTime: number
  energyPerTick: number
  blockId: number
  casingId: number

  // Blocks
  blocks: Map<string, BlockEntry>
  selectedTool: string
  selectedBlock: string | null
  layerFilter: number // -1 = all

  // GUI
  guiComponents: GuiComponent[]
  selectedCompIndex: number
  snapEnabled: boolean
  gridSize: number

  // UI
  activeTab: 'structure' | 'gui'
  showBuildGuide: boolean
  mcpConnected: boolean

  // Actions: config
  setName: (name: string) => void
  setStructType: (type: StructureType) => void
  setDimensions: (w: number, h: number, d: number) => void
  setIOTypes: (types: IOType[]) => void
  toggleIOType: (type: IOType) => void
  setCapacity: (key: 'energy' | 'fluid' | 'gas', value: number) => void
  setProcessTime: (v: number) => void
  setEnergyPerTick: (v: number) => void
  setBlockId: (v: number) => void
  setCasingId: (v: number) => void

  // Actions: blocks
  placeBlock: (x: number, y: number, z: number, type: string, mode?: PortMode) => void
  removeBlock: (x: number, y: number, z: number) => void
  fillShell: () => void
  clearBlocks: () => void
  setSelectedTool: (tool: string) => void
  setSelectedBlock: (key: string | null) => void
  setLayerFilter: (layer: number) => void

  // Actions: GUI
  addGuiComponent: (type: GuiComponentType, x: number, y: number, extra?: { w?: number; h?: number; slotType?: SlotRole; ioMode?: IoMode }) => void
  removeGuiComponent: (index: number) => void
  updateGuiComponent: (index: number, updates: Partial<GuiComponent>) => void
  clearGui: () => void
  loadGuiPreset: (preset: string) => void
  setSelectedComp: (index: number) => void
  setSnapEnabled: (v: boolean) => void
  setGridSize: (v: number) => void

  // Actions: UI
  setActiveTab: (tab: 'structure' | 'gui') => void
  setShowBuildGuide: (v: boolean) => void
  setMcpConnected: (v: boolean) => void

  // Actions: serialize/deserialize
  serialize: () => SerializedMultiblock
  deserialize: (data: SerializedMultiblock) => void
  exportJSON: () => Promise<void>
  importJSON: () => Promise<void>
  newProject: () => void
}

function snap(val: number, enabled: boolean, gridSize: number): number {
  if (!enabled || gridSize <= 1) return Math.round(val)
  return Math.round(val / gridSize) * gridSize
}

export const useStore = create<MultiblockStore>((set, get) => ({
  // Initial state
  name: 'MegaCrusher',
  structType: 'machine',
  dimensions: { w: 3, h: 3, d: 3 },
  ioTypes: ['energy', 'item'],
  capacity: { energy: 64000, fluid: 0, gas: 0 },
  processTime: 200,
  energyPerTick: 32,
  blockId: 213,
  casingId: 214,
  blocks: new Map(),
  selectedTool: 'casing',
  selectedBlock: null,
  layerFilter: -1,
  guiComponents: [],
  selectedCompIndex: -1,
  snapEnabled: true,
  gridSize: 9,
  activeTab: 'structure',

  // Config
  setName: (name) => set({ name }),
  setStructType: (structType) => set({ structType }),
  setDimensions: (w, h, d) => set({ dimensions: { w, h, d } }),
  setIOTypes: (ioTypes) => set({ ioTypes }),
  toggleIOType: (type) => set((s) => ({
    ioTypes: s.ioTypes.includes(type)
      ? s.ioTypes.filter((t) => t !== type)
      : [...s.ioTypes, type],
  })),
  setCapacity: (key, value) => set((s) => ({ capacity: { ...s.capacity, [key]: value } })),
  setProcessTime: (processTime) => set({ processTime }),
  setEnergyPerTick: (energyPerTick) => set({ energyPerTick }),
  setBlockId: (blockId) => set({ blockId }),
  setCasingId: (casingId) => set({ casingId }),

  // Blocks
  placeBlock: (x, y, z, type, mode = 'input_output') => set((s) => {
    const { w, h, d } = s.dimensions
    if (x < 0 || x >= w || y < 0 || y >= h || z < 0 || z >= d) return s
    const blocks = new Map(s.blocks)
    blocks.set(`${x},${y},${z}`, { type, mode })

    // Auto-link: placing a port auto-activates its IO type
    const def = blockRegistry.get(type)
    if (def?.portType && !s.ioTypes.includes(def.portType)) {
      return { blocks, ioTypes: [...s.ioTypes, def.portType] }
    }
    return { blocks }
  }),
  removeBlock: (x, y, z) => set((s) => {
    const blocks = new Map(s.blocks)
    blocks.delete(`${x},${y},${z}`)
    return { blocks }
  }),
  fillShell: () => set((s) => {
    const { w, h, d } = s.dimensions
    const blocks = new Map(s.blocks)
    for (let y = 0; y < h; y++)
      for (let z = 0; z < d; z++)
        for (let x = 0; x < w; x++) {
          const isEdge = x === 0 || x === w - 1 || y === 0 || y === h - 1 || z === 0 || z === d - 1
          if (isEdge && !blocks.has(`${x},${y},${z}`))
            blocks.set(`${x},${y},${z}`, { type: 'casing', mode: 'input_output' })
        }
    return { blocks }
  }),
  clearBlocks: () => set({ blocks: new Map(), selectedBlock: null }),
  setSelectedTool: (selectedTool) => set({ selectedTool }),
  setSelectedBlock: (selectedBlock) => set({ selectedBlock }),
  setLayerFilter: (layerFilter) => set({ layerFilter }),

  // GUI
  addGuiComponent: (type, x, y, extra) => set((s) => {
    const def = GUI_COMP_DEFS[type]
    if (!def) return s
    const sx = snap(x, s.snapEnabled, s.gridSize)
    const sy = snap(y, s.snapEnabled, s.gridSize)
    const comp: GuiComponent = {
      type,
      x: Math.max(3, Math.min(GUI_W - (extra?.w || def.w) - 3, sx)),
      y: Math.max(3, Math.min(GUI_H - (extra?.h || def.h) - 3, sy)),
      w: extra?.w || def.w,
      h: extra?.h || def.h,
      slotType: extra?.slotType || def.slotType || null,
      ioMode: extra?.ioMode || def.ioMode,
    }
    const guiComponents = [...s.guiComponents, comp]
    return { guiComponents, selectedCompIndex: guiComponents.length - 1 }
  }),
  removeGuiComponent: (index) => set((s) => {
    const guiComponents = s.guiComponents.filter((_, i) => i !== index)
    return { guiComponents, selectedCompIndex: -1 }
  }),
  updateGuiComponent: (index, updates) => set((s) => {
    const guiComponents = s.guiComponents.map((c, i) => i === index ? { ...c, ...updates } : c)
    return { guiComponents }
  }),
  clearGui: () => set({ guiComponents: [], selectedCompIndex: -1 }),
  loadGuiPreset: (preset) => {
    const s = get()
    s.clearGui()
    switch (preset) {
      case 'processor':
        s.addGuiComponent('slot', 55, 34, { slotType: 'input' })
        s.addGuiComponent('progress_arrow', 79, 35)
        s.addGuiComponent('slot', 115, 34, { slotType: 'output' })
        s.addGuiComponent('energy_bar', 10, 16)
        break
      case 'dual_input':
        s.addGuiComponent('slot', 45, 25, { slotType: 'input' })
        s.addGuiComponent('slot', 45, 47, { slotType: 'input' })
        s.addGuiComponent('progress_arrow', 73, 35)
        s.addGuiComponent('big_slot', 107, 30, { slotType: 'output' })
        s.addGuiComponent('energy_bar', 10, 16)
        break
      case 'single_slot':
        s.addGuiComponent('slot', 79, 34, { slotType: 'input' })
        s.addGuiComponent('energy_bar', 161, 16)
        break
      case 'tank':
        s.addGuiComponent('fluid_tank', 80, 16)
        s.addGuiComponent('energy_bar', 10, 16)
        break
    }
    set({ selectedCompIndex: -1 })
  },
  setSelectedComp: (selectedCompIndex) => set({ selectedCompIndex }),
  setSnapEnabled: (snapEnabled) => set({ snapEnabled }),
  setGridSize: (gridSize) => set({ gridSize }),

  // UI
  setActiveTab: (activeTab) => set({ activeTab }),
  showBuildGuide: false,
  setShowBuildGuide: (showBuildGuide) => set({ showBuildGuide }),
  mcpConnected: false,
  setMcpConnected: (mcpConnected) => set({ mcpConnected }),

  // Serialize: convert store state to JSON format matching MCP server
  serialize: () => {
    const s = get()
    const { w, h, d } = s.dimensions
    const layers: { layer: number; pattern: string[][] }[] = []
    const legend: Record<string, string> = { ' ': 'air' }

    // Build char lookup from block registry (includes custom blocks)
    const typeToChar: Record<string, string> = {}
    for (const [id, def] of blockRegistry) {
      typeToChar[id] = def.char
    }

    for (let y = 0; y < h; y++) {
      const pattern: string[][] = []
      for (let z = 0; z < d; z++) {
        const row: string[] = []
        for (let x = 0; x < w; x++) {
          const block = s.blocks.get(`${x},${y},${z}`)
          if (block) {
            const ch = typeToChar[block.type] || ' '
            row.push(ch)
            if (ch !== ' ') legend[ch] = block.type
          } else {
            row.push(' ')
          }
        }
        pattern.push(row)
      }
      layers.push({ layer: y, pattern })
    }

    const portModes: Record<string, PortMode> = {}
    for (const [key, block] of s.blocks) {
      const def = blockRegistry.get(block.type)
      if (def?.category === 'port' || block.type.endsWith('_port')) {
        portModes[key] = block.mode
      }
    }

    // Include custom blocks in registry field
    const customBlocks = [...blockRegistry.values()].filter(b => !b.builtIn)

    return {
      name: s.name,
      structType: s.structType,
      dimensions: [w, h, d] as [number, number, number],
      ioTypes: s.ioTypes,
      capacity: s.capacity,
      processTime: s.processTime,
      energyPerTick: s.energyPerTick,
      blockId: s.blockId,
      casingId: s.casingId,
      structure: layers,
      legend,
      portModes,
      guiComponents: s.guiComponents,
      ...(customBlocks.length > 0 ? { registry: customBlocks } : {}),
    }
  },

  // Deserialize: load from JSON format (from MCP server or file)
  deserialize: (data) => {
    // Register custom blocks from the saved registry before loading structure
    if (data.registry) {
      for (const def of data.registry) {
        if (!blockRegistry.has(def.id)) {
          blockRegistry.set(def.id, def)
        }
      }
    }

    const charToType: Record<string, string> = {}
    for (const [ch, type] of Object.entries(data.legend)) {
      charToType[ch] = type
    }

    const blocks = new Map<string, BlockEntry>()
    for (const layer of data.structure) {
      for (let z = 0; z < layer.pattern.length; z++) {
        for (let x = 0; x < layer.pattern[z].length; x++) {
          const ch = layer.pattern[z][x]
          const type = charToType[ch]
          if (type && type !== 'air') {
            const key = `${x},${layer.layer},${z}`
            const mode = data.portModes?.[key] || 'input_output'
            blocks.set(key, { type, mode })
          }
        }
      }
    }

    set({
      name: data.name,
      structType: data.structType,
      dimensions: { w: data.dimensions[0], h: data.dimensions[1], d: data.dimensions[2] },
      ioTypes: data.ioTypes,
      capacity: { ...data.capacity },
      processTime: data.processTime,
      energyPerTick: data.energyPerTick,
      blockId: data.blockId,
      casingId: data.casingId,
      guiComponents: data.guiComponents ? data.guiComponents.map((c) => ({
        ...c,
        ioMode: c.ioMode || GUI_COMP_DEFS[c.type]?.ioMode || 'display',
      })) : [],
      blocks,
      selectedBlock: null,
      selectedCompIndex: -1,
    })
  },

  // Export JSON via Electron file dialog
  exportJSON: async () => {
    const api = (window as any).api
    if (!api) return
    const data = get().serialize()
    const filePath = await api.saveDialog(`${data.name}.json`, [{ name: 'JSON', extensions: ['json'] }])
    if (filePath) {
      await api.saveFile(filePath, JSON.stringify(data, null, 2))
    }
  },

  // Import JSON via Electron file dialog
  importJSON: async () => {
    const api = (window as any).api
    if (!api) return
    const filePath = await api.openDialog([{ name: 'JSON', extensions: ['json'] }])
    if (filePath) {
      const content = await api.readFile(filePath)
      if (content) {
        try {
          const data = JSON.parse(content) as SerializedMultiblock
          get().deserialize(data)
        } catch { /* ignore parse errors */ }
      }
    }
  },

  // Reset to defaults
  newProject: () => set({
    name: 'Unnamed',
    structType: 'machine',
    dimensions: { w: 3, h: 3, d: 3 },
    ioTypes: ['energy', 'item'],
    capacity: { energy: 64000, fluid: 0, gas: 0 },
    processTime: 200,
    energyPerTick: 32,
    blockId: 213,
    casingId: 214,
    blocks: new Map(),
    guiComponents: [],
    selectedBlock: null,
    selectedCompIndex: -1,
    selectedTool: 'casing',
    layerFilter: -1,
  }),
}))
