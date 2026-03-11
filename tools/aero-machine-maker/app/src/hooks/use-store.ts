import { create } from 'zustand'
import { toast } from 'sonner'
import type { BlockType, StructureType, IOType, PortMode, BlockEntry, GuiComponent, GuiComponentType, SlotRole, IoMode, BlockDef, BlockCategory } from '@/core/types'
import { GUI_COMP_DEFS, blockRegistry, getBlockInfo } from '@/core/types'

const GUI_W = 176
const GUI_H = 166

// Serialized JSON format (matches MCP server output)
export interface SerializedMultiblock {
  projectType?: ProjectType
  name: string
  structType: StructureType
  dimensions: [number, number, number]
  ioTypes: IOType[]
  capacity: { energy: number; fluid: number; gas: number }
  processTime: number
  energyPerTick: number
  blockId: number
  defaultShellBlock?: string
  controllerPos?: string
  structure: { layer: number; pattern: string[][] }[]
  legend: Record<string, string>
  portModes: Record<string, PortMode>
  portTypes?: Record<string, IOType>
  guiComponents: GuiComponent[]
  model?: { name: string; elements: { name: string; from: [number, number, number]; to: [number, number, number] }[]; textureName: string }
  registry?: BlockDef[]
  /** @deprecated Legacy field — ignored on load */
  casingId?: number
}

// Camera command from MCP WebSocket
export interface CameraCommand {
  theta?: number
  phi?: number
  radius?: number
}

// Highlight command from MCP WebSocket
export interface HighlightCommand {
  keys: string[]
  duration: number
}

export type ProjectType = 'single' | 'multiblock'

// Animation types (matches MCP server)
export interface AnimStateMapping {
  stateId: number
  label: string
  clipName: string
}

export interface AnimationConfig {
  objPath: string | null
  objContent: string | null
  bbmodelPath: string | null
  texturePath: string | null
  textureDataUrl: string | null
  animJson: any | null
  clipNames: string[]
  boneNames: string[]
  stateMappings: AnimStateMapping[]
}

interface MultiblockStore {
  // Project type
  projectType: ProjectType

  // Structure config
  name: string
  structType: StructureType
  dimensions: { w: number; h: number; d: number }
  ioTypes: IOType[]
  capacity: { energy: number; fluid: number; gas: number }
  processTime: number
  energyPerTick: number
  blockId: number
  defaultShellBlock: string

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

  // Animation
  animConfig: AnimationConfig

  // UI
  activeTab: 'structure' | 'gui' | 'model'
  showBuildGuide: boolean
  mcpConnected: boolean

  // Block registry version (incremented when custom blocks are added/removed)
  registryVersion: number

  // MCP UI commands (consumed by structure-editor)
  pendingCamera: CameraCommand | null
  pendingHighlight: HighlightCommand | null

  // Actions: project
  setProjectType: (type: ProjectType) => void

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
  setDefaultShellBlock: (id: string) => void

  // Actions: blocks
  placeBlock: (x: number, y: number, z: number, type: string, mode?: PortMode) => void
  removeBlock: (x: number, y: number, z: number) => void
  fillShell: () => void
  clearBlocks: () => void
  setSelectedTool: (tool: string) => void
  setSelectedBlock: (key: string | null) => void
  setPortType: (key: string, portType: IOType | null) => void
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

  // Actions: animation
  setAnimConfig: (config: AnimationConfig) => void

  // Actions: UI
  setActiveTab: (tab: 'structure' | 'gui' | 'model') => void
  setShowBuildGuide: (v: boolean) => void
  setMcpConnected: (v: boolean) => void

  // Actions: MCP UI commands
  setCameraCommand: (cmd: CameraCommand) => void
  consumeCameraCommand: () => CameraCommand | null
  setHighlightCommand: (cmd: HighlightCommand) => void
  consumeHighlightCommand: () => HighlightCommand | null

  // Actions: block registry
  registerBlock: (def: BlockDef) => void
  unregisterBlock: (id: string) => void

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
  projectType: 'multiblock',
  name: 'MegaCrusher',
  structType: 'machine',
  dimensions: { w: 3, h: 3, d: 3 },
  ioTypes: ['energy', 'item'],
  capacity: { energy: 64000, fluid: 0, gas: 0 },
  processTime: 200,
  energyPerTick: 32,
  blockId: 213,
  defaultShellBlock: 'iron_block',
  blocks: new Map(),
  selectedTool: 'iron_block',
  selectedBlock: null,
  layerFilter: -1,
  guiComponents: [],
  selectedCompIndex: -1,
  snapEnabled: true,
  gridSize: 9,
  animConfig: {
    objPath: null, objContent: null, bbmodelPath: null, texturePath: null, textureDataUrl: null,
    animJson: null, clipNames: [], boneNames: [], stateMappings: [],
  },
  activeTab: 'structure',
  registryVersion: 0,
  pendingCamera: null,
  pendingHighlight: null,

  // Project
  setProjectType: (projectType) => set({ projectType }),

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
  setDefaultShellBlock: (defaultShellBlock) => set({ defaultShellBlock }),

  // Blocks
  placeBlock: (x, y, z, type, mode = 'input_output') => {
    const s = get()
    const { w, h, d } = s.dimensions
    if (x < 0 || x >= w || y < 0 || y >= h || z < 0 || z >= d) return
    const key = `${x},${y},${z}`

    // Only one controller block allowed (any block with category 'controller')
    const placingDef = blockRegistry.get(type)
    if (placingDef?.category === 'controller') {
      for (const [k, block] of s.blocks) {
        if (k === key) continue
        const existingDef = blockRegistry.get(block.type)
        if (existingDef?.category === 'controller') {
          toast.error('Only one controller per structure', {
            description: `Remove controller at ${k} first`,
          })
          return
        }
      }
    }

    const blocks = new Map(s.blocks)
    blocks.set(key, { type, mode })

    // Auto-link: placing a port auto-activates its IO type
    const def = blockRegistry.get(type)
    if (def?.portType && !s.ioTypes.includes(def.portType)) {
      set({ blocks, ioTypes: [...s.ioTypes, def.portType] })
    } else {
      set({ blocks })
    }
  },
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
            blocks.set(`${x},${y},${z}`, { type: s.defaultShellBlock, mode: 'input_output' })
        }
    return { blocks }
  }),
  clearBlocks: () => set({ blocks: new Map(), selectedBlock: null }),
  setSelectedTool: (selectedTool) => set({ selectedTool }),
  setSelectedBlock: (selectedBlock) => set({ selectedBlock }),
  setPortType: (key, portType) => set((s) => {
    const block = s.blocks.get(key)
    if (!block) return s
    const blocks = new Map(s.blocks)
    if (portType) {
      blocks.set(key, { ...block, portType })
      // Auto-activate IO type
      if (!s.ioTypes.includes(portType)) {
        return { blocks, ioTypes: [...s.ioTypes, portType] }
      }
    } else {
      const { portType: _, ...rest } = block
      blocks.set(key, rest as BlockEntry)
    }
    return { blocks }
  }),
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

  // Animation
  setAnimConfig: (animConfig) => set({ animConfig }),

  // UI
  setActiveTab: (activeTab) => set({ activeTab }),
  showBuildGuide: false,
  setShowBuildGuide: (showBuildGuide) => set({ showBuildGuide }),
  mcpConnected: false,
  setMcpConnected: (mcpConnected) => set({ mcpConnected }),

  // MCP UI commands
  setCameraCommand: (cmd) => set({ pendingCamera: cmd }),
  consumeCameraCommand: () => {
    const cmd = get().pendingCamera
    if (cmd) set({ pendingCamera: null })
    return cmd
  },
  setHighlightCommand: (cmd) => set({ pendingHighlight: cmd }),
  consumeHighlightCommand: () => {
    const cmd = get().pendingHighlight
    if (cmd) set({ pendingHighlight: null })
    return cmd
  },

  // Block registry management
  registerBlock: (def) => {
    if (!blockRegistry.has(def.id)) {
      blockRegistry.set(def.id, def)
      set((s) => ({ registryVersion: s.registryVersion + 1 }))
    }
  },
  unregisterBlock: (id) => {
    const def = blockRegistry.get(id)
    if (def && !def.builtIn) {
      blockRegistry.delete(id)
      set((s) => ({ registryVersion: s.registryVersion + 1 }))
    }
  },

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

    // Find controller position, port modes and port types
    let controllerPos: string | undefined
    const portModes: Record<string, PortMode> = {}
    const portTypes: Record<string, IOType> = {}
    for (const [key, block] of s.blocks) {
      const bDef = blockRegistry.get(block.type)
      if (bDef?.category === 'controller') controllerPos = key
      if (block.portType) {
        portModes[key] = block.mode
        portTypes[key] = block.portType
      }
    }

    // Include custom blocks in registry field
    const customBlocks = [...blockRegistry.values()].filter(b => !b.builtIn)

    return {
      projectType: s.projectType,
      name: s.name,
      structType: s.structType,
      dimensions: [w, h, d] as [number, number, number],
      ioTypes: s.ioTypes,
      capacity: s.capacity,
      processTime: s.processTime,
      energyPerTick: s.energyPerTick,
      blockId: s.blockId,
      defaultShellBlock: s.defaultShellBlock,
      controllerPos,
      structure: layers,
      legend,
      portModes,
      portTypes: Object.keys(portTypes).length > 0 ? portTypes : undefined,
      guiComponents: s.guiComponents,
      ...(customBlocks.length > 0 ? { registry: customBlocks } : {}),
    }
  },

  // Deserialize: load from JSON format (from MCP server or file)
  deserialize: (data) => {
    // Register custom blocks from the saved registry before loading structure
    let registryChanged = false
    if (data.registry) {
      for (const def of data.registry) {
        if (!blockRegistry.has(def.id)) {
          blockRegistry.set(def.id, def)
          registryChanged = true
        }
      }
    }

    const charToType: Record<string, string> = {}
    for (const [ch, type] of Object.entries(data.legend)) {
      // Legacy compat: old 'casing' → defaultShellBlock
      if (type === 'casing') {
        charToType[ch] = data.defaultShellBlock || 'iron_block'
      } else {
        charToType[ch] = type
      }
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
            const entry: BlockEntry = { type, mode }
            if (data.portTypes?.[key]) entry.portType = data.portTypes[key]
            blocks.set(key, entry)
          }
        }
      }
    }

    // Legacy compat: portTypes on air positions → create machine_port blocks
    if (data.portTypes) {
      for (const [key, ioType] of Object.entries(data.portTypes)) {
        if (!blocks.has(key)) {
          const mode = (data.portModes?.[key] || 'input_output') as PortMode
          blocks.set(key, { type: 'machine_port', mode, portType: ioType as IOType })
        }
      }
    }

    set((s) => ({
      projectType: data.projectType || 'multiblock',
      name: data.name,
      structType: data.structType,
      dimensions: { w: data.dimensions[0], h: data.dimensions[1], d: data.dimensions[2] },
      ioTypes: data.ioTypes,
      capacity: { ...data.capacity },
      processTime: data.processTime,
      energyPerTick: data.energyPerTick,
      blockId: data.blockId,
      defaultShellBlock: data.defaultShellBlock || 'iron_block',
      guiComponents: data.guiComponents ? data.guiComponents.map((c) => ({
        ...c,
        ioMode: c.ioMode || GUI_COMP_DEFS[c.type]?.ioMode || 'display',
      })) : [],
      blocks,
      selectedBlock: null,
      selectedCompIndex: -1,
      ...(registryChanged ? { registryVersion: s.registryVersion + 1 } : {}),
    }))
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
    projectType: 'multiblock',
    name: 'Unnamed',
    structType: 'machine',
    dimensions: { w: 3, h: 3, d: 3 },
    ioTypes: ['energy', 'item'],
    capacity: { energy: 64000, fluid: 0, gas: 0 },
    processTime: 200,
    energyPerTick: 32,
    blockId: 213,
    defaultShellBlock: 'iron_block',
    blocks: new Map(),
    guiComponents: [],
    selectedBlock: null,
    selectedCompIndex: -1,
    selectedTool: 'iron_block',
    layerFilter: -1,
    animConfig: {
      objPath: null, objContent: null, bbmodelPath: null,
      animJson: null, clipNames: [], boneNames: [], stateMappings: [],
      texturePath: null, textureDataUrl: null,
    },
  }),
}))
