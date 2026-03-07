import { DEFAULT_BLOCKS } from '../../shared/block-defaults.js';

// Block category determines behavior in codegen and structure validation
export type BlockCategory = 'casing' | 'controller' | 'port' | 'glass' | 'custom';
export type StructureType = 'machine' | 'tank' | 'reactor' | 'custom';
export type PortMode = 'input' | 'output' | 'input_output';
export type IOType = 'energy' | 'fluid' | 'gas' | 'item';
export type SlotType = 'input' | 'output' | 'fuel';

export type GuiComponentType =
  | 'slot' | 'big_slot' | 'energy_bar' | 'progress_arrow'
  | 'flame' | 'fluid_tank' | 'gas_tank' | 'separator';

export const GUI_COMP_DEFS: Record<GuiComponentType, { w: number; h: number; slotType?: SlotType }> = {
  slot:           { w: 18, h: 18, slotType: 'input' },
  big_slot:       { w: 26, h: 26, slotType: 'output' },
  energy_bar:     { w: 8,  h: 54 },
  progress_arrow: { w: 24, h: 17 },
  flame:          { w: 14, h: 14 },
  fluid_tank:     { w: 18, h: 54 },
  gas_tank:       { w: 18, h: 54 },
  separator:      { w: 162, h: 2 },
};

export interface BlockDef {
  id: string;
  category: BlockCategory;
  label: string;
  color: number;
  char: string;
  portType?: IOType;
  tier?: number;
  builtIn: boolean;
}

export interface BlockEntry {
  blockId: string;
  mode: PortMode;
}

export interface GuiComponent {
  type: GuiComponentType;
  x: number;
  y: number;
  w: number;
  h: number;
  slotType: SlotType | null;
}

export interface Capacity {
  energy: number;
  fluid: number;
  gas: number;
}

export interface ModelElement {
  name: string;
  from: [number, number, number];
  to: [number, number, number];
}

export interface BlockModel {
  name: string;
  elements: ModelElement[];
  textureName: string;
}

export interface MultiblockState {
  name: string;
  structType: StructureType;
  dimensions: { w: number; h: number; d: number };
  blocks: Map<string, BlockEntry>;
  ioTypes: IOType[];
  capacity: Capacity;
  processTime: number;
  energyPerTick: number;
  blockId: number;
  casingId: number;
  guiComponents: GuiComponent[];
  model: BlockModel | null;
}

export interface GeneratedFile {
  name: string;
  relativePath: string;
  content: string;
}

export interface StructureLayer {
  layer: number;
  pattern: string[][];
}

export interface SerializedMultiblock {
  name: string;
  structType: StructureType;
  dimensions: [number, number, number];
  ioTypes: IOType[];
  capacity: Capacity;
  processTime: number;
  energyPerTick: number;
  blockId: number;
  casingId: number;
  structure: StructureLayer[];
  legend: Record<string, string>;
  portModes: Record<string, PortMode>;
  guiComponents: GuiComponent[];
  model?: BlockModel;
  registry?: BlockDef[];
}

// ---------------------------------------------------------------------------
// Block Registry
// ---------------------------------------------------------------------------

const CHAR_POOL = 'ABDHJLMNOPQRSTUVXYZ0123456789';

export class BlockRegistry {
  private blocks = new Map<string, BlockDef>();
  private charToId = new Map<string, string>();

  constructor() {
    this.registerDefaults();
  }

  private registerDefaults(): void {
    for (const b of DEFAULT_BLOCKS) {
      this.register({
        id: b.id,
        category: b.category as BlockCategory,
        label: b.label,
        color: b.color,
        char: b.char,
        portType: b.portType as IOType | undefined,
        builtIn: true,
      });
    }
  }

  register(def: BlockDef): void {
    if (this.blocks.has(def.id)) {
      throw new Error(`Block ID "${def.id}" already registered`);
    }
    if (this.charToId.has(def.char)) {
      throw new Error(`Char "${def.char}" already used by block "${this.charToId.get(def.char)}"`);
    }
    this.blocks.set(def.id, def);
    this.charToId.set(def.char, def.id);
  }

  unregister(id: string): boolean {
    const def = this.blocks.get(id);
    if (!def) return false;
    if (def.builtIn) return false;
    this.charToId.delete(def.char);
    this.blocks.delete(id);
    return true;
  }

  has(id: string): boolean { return this.blocks.has(id); }
  get(id: string): BlockDef | undefined { return this.blocks.get(id); }
  getByChar(char: string): BlockDef | undefined {
    const id = this.charToId.get(char);
    return id ? this.blocks.get(id) : undefined;
  }
  getAll(): BlockDef[] { return [...this.blocks.values()]; }
  getIds(): string[] { return [...this.blocks.keys()]; }

  getCustom(): BlockDef[] {
    return this.getAll().filter(b => !b.builtIn);
  }

  nextAvailableChar(): string | null {
    for (const ch of CHAR_POOL) {
      if (!this.charToId.has(ch)) return ch;
    }
    return null;
  }

  reset(): void {
    this.blocks.clear();
    this.charToId.clear();
    this.registerDefaults();
  }
}

export const blockRegistry = new BlockRegistry();
