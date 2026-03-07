import {
  MultiblockState, BlockEntry, StructureType, IOType,
  PortMode, GuiComponent, GuiComponentType, SlotType,
  GUI_COMP_DEFS, SerializedMultiblock, StructureLayer,
  blockRegistry, BlockDef, BlockModel, ModelElement,
} from './types.js';

const GUI_W = 176;
const GUI_H = 166;

export function createDefaultState(): MultiblockState {
  return {
    name: 'Unnamed',
    structType: 'machine',
    dimensions: { w: 3, h: 3, d: 3 },
    blocks: new Map(),
    ioTypes: ['energy', 'item'],
    capacity: { energy: 64000, fluid: 0, gas: 0 },
    processTime: 200,
    energyPerTick: 32,
    blockId: 213,
    casingId: 214,
    guiComponents: [],
    model: null,
  };
}

let state: MultiblockState = createDefaultState();

// Change listener for real-time sync (WebSocket broadcast)
let onChangeListener: (() => void) | null = null;
export function setOnChangeListener(fn: () => void): void {
  onChangeListener = fn;
}
function notifyChange(): void {
  if (onChangeListener) onChangeListener();
}

export function getState(): MultiblockState {
  return state;
}

export function resetState(): void {
  state = createDefaultState();
  notifyChange();
}

export function configureMultiblock(opts: {
  name?: string;
  structType?: StructureType;
  w?: number; h?: number; d?: number;
  ioTypes?: IOType[];
  capacity?: Partial<MultiblockState['capacity']>;
  processTime?: number;
  energyPerTick?: number;
  blockId?: number;
  casingId?: number;
}): void {
  if (opts.name) state.name = opts.name;
  if (opts.structType) state.structType = opts.structType;
  if (opts.w) state.dimensions.w = opts.w;
  if (opts.h) state.dimensions.h = opts.h;
  if (opts.d) state.dimensions.d = opts.d;
  if (opts.ioTypes) state.ioTypes = opts.ioTypes;
  if (opts.capacity) state.capacity = { ...state.capacity, ...opts.capacity };
  if (opts.processTime) state.processTime = opts.processTime;
  if (opts.energyPerTick) state.energyPerTick = opts.energyPerTick;
  if (opts.blockId) state.blockId = opts.blockId;
  if (opts.casingId) state.casingId = opts.casingId;
  notifyChange();
}

export function placeBlock(x: number, y: number, z: number, type: string, mode: PortMode = 'input_output'): boolean {
  const { w, h, d } = state.dimensions;
  if (x < 0 || x >= w || y < 0 || y >= h || z < 0 || z >= d) return false;
  state.blocks.set(`${x},${y},${z}`, { blockId: type, mode });
  notifyChange();
  return true;
}

export function removeBlock(x: number, y: number, z: number): boolean {
  const result = state.blocks.delete(`${x},${y},${z}`);
  if (result) notifyChange();
  return result;
}

export function fillShell(): number {
  const { w, h, d } = state.dimensions;
  let count = 0;
  for (let y = 0; y < h; y++) {
    for (let z = 0; z < d; z++) {
      for (let x = 0; x < w; x++) {
        const isEdge = x === 0 || x === w - 1 || y === 0 || y === h - 1 || z === 0 || z === d - 1;
        if (isEdge) {
          const key = `${x},${y},${z}`;
          if (!state.blocks.has(key)) {
            state.blocks.set(key, { blockId: 'casing', mode: 'input_output' });
            count++;
          }
        }
      }
    }
  }
  notifyChange();
  return count;
}

export function clearBlocks(): void {
  state.blocks.clear();
  notifyChange();
}

export function addGuiComponent(type: GuiComponentType, x: number, y: number, extra?: {
  w?: number; h?: number; slotType?: SlotType;
}): number {
  const def = GUI_COMP_DEFS[type];
  if (!def) return -1;
  const comp: GuiComponent = {
    type,
    x: Math.max(3, Math.min(GUI_W - (extra?.w || def.w) - 3, x)),
    y: Math.max(3, Math.min(GUI_H - (extra?.h || def.h) - 3, y)),
    w: extra?.w || def.w,
    h: extra?.h || def.h,
    slotType: extra?.slotType || def.slotType || null,
  };
  state.guiComponents.push(comp);
  notifyChange();
  return state.guiComponents.length - 1;
}

export function clearGui(): void {
  state.guiComponents = [];
  notifyChange();
}

export function loadGuiPreset(preset: string): void {
  clearGui();
  switch (preset) {
    case 'processor':
      addGuiComponent('slot', 55, 34, { slotType: 'input' });
      addGuiComponent('progress_arrow', 79, 35);
      addGuiComponent('slot', 115, 34, { slotType: 'output' });
      addGuiComponent('energy_bar', 10, 16);
      break;
    case 'dual_input':
      addGuiComponent('slot', 45, 25, { slotType: 'input' });
      addGuiComponent('slot', 45, 47, { slotType: 'input' });
      addGuiComponent('progress_arrow', 73, 35);
      addGuiComponent('big_slot', 107, 30, { slotType: 'output' });
      addGuiComponent('energy_bar', 10, 16);
      break;
    case 'single_slot':
      addGuiComponent('slot', 79, 34, { slotType: 'input' });
      addGuiComponent('energy_bar', 161, 16);
      break;
    case 'tank':
      addGuiComponent('fluid_tank', 80, 16);
      addGuiComponent('energy_bar', 10, 16);
      break;
  }
}

export function serialize(): SerializedMultiblock {
  const { w, h, d } = state.dimensions;
  const layers: StructureLayer[] = [];
  const legend: Record<string, string> = { ' ': 'air' };

  for (let y = 0; y < h; y++) {
    const pattern: string[][] = [];
    for (let z = 0; z < d; z++) {
      const row: string[] = [];
      for (let x = 0; x < w; x++) {
        const block = state.blocks.get(`${x},${y},${z}`);
        if (block) {
          const info = blockRegistry.get(block.blockId);
          if (info) {
            row.push(info.char);
            legend[info.char] = block.blockId;
          } else {
            row.push(' ');
          }
        } else {
          row.push(' ');
        }
      }
      pattern.push(row);
    }
    layers.push({ layer: y, pattern });
  }

  const portModes: Record<string, PortMode> = {};
  for (const [key, block] of state.blocks) {
    const def = blockRegistry.get(block.blockId);
    if (def && def.category === 'port') {
      portModes[key] = block.mode;
    }
  }

  const customBlocks = blockRegistry.getCustom();

  return {
    name: state.name,
    structType: state.structType,
    dimensions: [w, h, d],
    ioTypes: state.ioTypes,
    capacity: state.capacity,
    processTime: state.processTime,
    energyPerTick: state.energyPerTick,
    blockId: state.blockId,
    casingId: state.casingId,
    structure: layers,
    legend,
    portModes,
    guiComponents: state.guiComponents,
    ...(state.model ? { model: state.model } : {}),
    ...(customBlocks.length > 0 ? { registry: customBlocks } : {}),
  };
}

export function getSlotInfo(): { inputs: { x: number; y: number; big: boolean }[]; outputs: { x: number; y: number; big: boolean }[] } {
  const inputs: { x: number; y: number; big: boolean }[] = [];
  const outputs: { x: number; y: number; big: boolean }[] = [];
  for (const comp of state.guiComponents) {
    if (comp.type === 'slot' || comp.type === 'big_slot') {
      const entry = { x: comp.x + 1, y: comp.y + 1, big: comp.type === 'big_slot' };
      if (comp.slotType === 'output') outputs.push(entry);
      else inputs.push(entry);
    }
  }
  return { inputs, outputs };
}

export function getComponentByType(type: GuiComponentType): GuiComponent | undefined {
  return state.guiComponents.find(c => c.type === type);
}

export function setDimensions(w: number, h: number, d: number): number {
  state.dimensions = { w, h, d };
  let removed = 0;
  for (const key of [...state.blocks.keys()]) {
    const [bx, by, bz] = key.split(',').map(Number);
    if (bx >= w || by >= h || bz >= d) {
      state.blocks.delete(key);
      removed++;
    }
  }
  notifyChange();
  return removed;
}

export function updateConfig(opts: {
  name?: string;
  structType?: StructureType;
  ioTypes?: IOType[];
  energyCapacity?: number;
  fluidCapacity?: number;
  gasCapacity?: number;
  processTime?: number;
  energyPerTick?: number;
  blockId?: number;
  casingId?: number;
}): void {
  if (opts.name !== undefined) state.name = opts.name;
  if (opts.structType !== undefined) state.structType = opts.structType;
  if (opts.ioTypes !== undefined) state.ioTypes = opts.ioTypes;
  if (opts.energyCapacity !== undefined) state.capacity.energy = opts.energyCapacity;
  if (opts.fluidCapacity !== undefined) state.capacity.fluid = opts.fluidCapacity;
  if (opts.gasCapacity !== undefined) state.capacity.gas = opts.gasCapacity;
  if (opts.processTime !== undefined) state.processTime = opts.processTime;
  if (opts.energyPerTick !== undefined) state.energyPerTick = opts.energyPerTick;
  if (opts.blockId !== undefined) state.blockId = opts.blockId;
  if (opts.casingId !== undefined) state.casingId = opts.casingId;
  notifyChange();
}

export function removeGuiComponent(index: number): boolean {
  if (index < 0 || index >= state.guiComponents.length) return false;
  state.guiComponents.splice(index, 1);
  notifyChange();
  return true;
}

export function updateGuiComponent(index: number, opts: {
  x?: number; y?: number; w?: number; h?: number; slotType?: SlotType;
}): boolean {
  if (index < 0 || index >= state.guiComponents.length) return false;
  const comp = state.guiComponents[index];
  if (opts.x !== undefined) comp.x = Math.max(3, Math.min(176 - comp.w - 3, opts.x));
  if (opts.y !== undefined) comp.y = Math.max(3, Math.min(166 - comp.h - 3, opts.y));
  if (opts.w !== undefined) comp.w = opts.w;
  if (opts.h !== undefined) comp.h = opts.h;
  if (opts.slotType !== undefined) comp.slotType = opts.slotType;
  notifyChange();
  return true;
}

export function deserialize(data: SerializedMultiblock): void {
  // Register custom blocks from the saved registry before loading structure
  if (data.registry) {
    for (const def of data.registry) {
      if (!blockRegistry.has(def.id)) {
        blockRegistry.register(def);
      }
    }
  }

  state.name = data.name;
  state.structType = data.structType;
  state.dimensions = { w: data.dimensions[0], h: data.dimensions[1], d: data.dimensions[2] };
  state.ioTypes = data.ioTypes;
  state.capacity = { ...data.capacity };
  state.processTime = data.processTime;
  state.energyPerTick = data.energyPerTick;
  state.blockId = data.blockId;
  state.casingId = data.casingId;
  state.guiComponents = data.guiComponents ? [...data.guiComponents] : [];
  state.model = data.model || null;

  state.blocks.clear();
  const charToType: Record<string, string> = {};
  for (const [ch, type] of Object.entries(data.legend)) {
    charToType[ch] = type;
  }
  for (const layer of data.structure) {
    for (let z = 0; z < layer.pattern.length; z++) {
      for (let x = 0; x < layer.pattern[z].length; x++) {
        const ch = layer.pattern[z][x];
        const type = charToType[ch];
        if (type && type !== 'air') {
          const key = `${x},${layer.layer},${z}`;
          const mode = data.portModes?.[key] || 'input_output';
          state.blocks.set(key, { blockId: type, mode: mode as PortMode });
        }
      }
    }
  }
  notifyChange();
}

// =========================================================================
// BATCH OPERATIONS
// =========================================================================

export function fillRegion(
  x1: number, y1: number, z1: number,
  x2: number, y2: number, z2: number,
  type: string, mode: PortMode = 'input_output', overwrite = false,
): number {
  const { w, h, d } = state.dimensions;
  let count = 0;
  for (let y = Math.max(0, y1); y <= Math.min(h - 1, y2); y++) {
    for (let z = Math.max(0, z1); z <= Math.min(d - 1, z2); z++) {
      for (let x = Math.max(0, x1); x <= Math.min(w - 1, x2); x++) {
        const key = `${x},${y},${z}`;
        if (overwrite || !state.blocks.has(key)) {
          state.blocks.set(key, { blockId: type, mode });
          count++;
        }
      }
    }
  }
  if (count > 0) notifyChange();
  return count;
}

export function replaceBlocks(fromType: string, toType: string, newMode?: PortMode): number {
  let count = 0;
  for (const [key, block] of state.blocks) {
    if (block.blockId === fromType) {
      block.blockId = toType;
      if (newMode !== undefined) block.mode = newMode;
      count++;
    }
  }
  if (count > 0) notifyChange();
  return count;
}

export function fillLayer(y: number, fillMode: 'shell' | 'all', type: string, mode: PortMode = 'input_output'): number {
  const { w, h, d } = state.dimensions;
  if (y < 0 || y >= h) return 0;
  let count = 0;
  for (let z = 0; z < d; z++) {
    for (let x = 0; x < w; x++) {
      if (fillMode === 'shell') {
        const isEdge = x === 0 || x === w - 1 || z === 0 || z === d - 1;
        if (!isEdge) continue;
      }
      const key = `${x},${y},${z}`;
      if (!state.blocks.has(key)) {
        state.blocks.set(key, { blockId: type, mode });
        count++;
      }
    }
  }
  if (count > 0) notifyChange();
  return count;
}

// =========================================================================
// LAYER OPERATIONS
// =========================================================================

export function copyLayer(srcY: number, dstY: number, overwrite = false): number {
  const { w, h, d } = state.dimensions;
  if (srcY < 0 || srcY >= h || dstY < 0 || dstY >= h) return 0;
  let count = 0;
  for (let z = 0; z < d; z++) {
    for (let x = 0; x < w; x++) {
      const srcKey = `${x},${srcY},${z}`;
      const dstKey = `${x},${dstY},${z}`;
      const srcBlock = state.blocks.get(srcKey);
      if (srcBlock && (overwrite || !state.blocks.has(dstKey))) {
        state.blocks.set(dstKey, { ...srcBlock });
        count++;
      }
    }
  }
  if (count > 0) notifyChange();
  return count;
}

export function mirrorStructure(axis: 'x' | 'z'): number {
  const { w, d } = state.dimensions;
  const newBlocks = new Map<string, BlockEntry>();
  let count = 0;
  for (const [key, block] of state.blocks) {
    const [x, y, z] = key.split(',').map(Number);
    let nx: number, nz: number;
    if (axis === 'x') {
      nx = w - 1 - x;
      nz = z;
    } else {
      nx = x;
      nz = d - 1 - z;
    }
    newBlocks.set(`${nx},${y},${nz}`, { ...block });
    count++;
  }
  state.blocks = newBlocks;
  notifyChange();
  return count;
}

export function rotateStructure(): { w: number; d: number; count: number } {
  const { w, d } = state.dimensions;
  const newBlocks = new Map<string, BlockEntry>();
  let count = 0;
  for (const [key, block] of state.blocks) {
    const [x, y, z] = key.split(',').map(Number);
    // 90° CW: (x,z) → (d-1-z, x)
    const nx = d - 1 - z;
    const nz = x;
    newBlocks.set(`${nx},${y},${nz}`, { ...block });
    count++;
  }
  // Swap w and d
  state.dimensions.w = d;
  state.dimensions.d = w;
  state.blocks = newBlocks;
  notifyChange();
  return { w: d, d: w, count };
}

// =========================================================================
// SMART PLACEMENT
// =========================================================================

export type Face = 'north' | 'south' | 'east' | 'west' | 'top' | 'bottom';

export function placeOnFace(
  face: Face, type: string, mode: PortMode = 'input_output', count: number = 0, replace: boolean = false,
): number {
  const { w, h, d } = state.dimensions;
  const candidates: string[] = [];

  for (const [key, block] of state.blocks) {
    const def = blockRegistry.get(block.blockId);
    if (!def) continue;
    // By default only replace casing; with replace=true, replace any block
    if (!replace && def.category !== 'casing') continue;
    const [x, y, z] = key.split(',').map(Number);
    let onFace = false;
    switch (face) {
      case 'north':  onFace = z === 0; break;
      case 'south':  onFace = z === d - 1; break;
      case 'west':   onFace = x === 0; break;
      case 'east':   onFace = x === w - 1; break;
      case 'top':    onFace = y === h - 1; break;
      case 'bottom': onFace = y === 0; break;
    }
    if (onFace) candidates.push(key);
  }

  // Sort candidates toward center for better placement
  candidates.sort((a, b) => {
    const [ax, ay, az] = a.split(',').map(Number);
    const [bx, by, bz] = b.split(',').map(Number);
    const aDist = Math.abs(ax - (w - 1) / 2) + Math.abs(ay - (h - 1) / 2) + Math.abs(az - (d - 1) / 2);
    const bDist = Math.abs(bx - (w - 1) / 2) + Math.abs(by - (h - 1) / 2) + Math.abs(bz - (d - 1) / 2);
    return aDist - bDist;
  });

  const toPlace = count <= 0 ? candidates.length : Math.min(count, candidates.length);
  let placed = 0;
  for (let i = 0; i < toPlace; i++) {
    state.blocks.set(candidates[i], { blockId: type, mode });
    placed++;
  }
  if (placed > 0) notifyChange();
  return placed;
}

// Keep backward compat alias
export const placePortsOnFace = placeOnFace;

export function placeRing(y: number, type: string, mode: PortMode = 'input_output'): number {
  const { w, h, d } = state.dimensions;
  if (y < 0 || y >= h) return 0;
  let count = 0;
  for (let x = 0; x < w; x++) {
    for (let z = 0; z < d; z++) {
      const isEdge = x === 0 || x === w - 1 || z === 0 || z === d - 1;
      if (isEdge) {
        const key = `${x},${y},${z}`;
        state.blocks.set(key, { blockId: type, mode });
        count++;
      }
    }
  }
  if (count > 0) notifyChange();
  return count;
}

// =========================================================================
// QUERY
// =========================================================================

export function getBlockAt(x: number, y: number, z: number): BlockEntry | null {
  return state.blocks.get(`${x},${y},${z}`) || null;
}

export function listBlocksByType(type: string): { x: number; y: number; z: number; mode: PortMode }[] {
  const result: { x: number; y: number; z: number; mode: PortMode }[] = [];
  for (const [key, block] of state.blocks) {
    if (block.blockId === type) {
      const [x, y, z] = key.split(',').map(Number);
      result.push({ x, y, z, mode: block.mode });
    }
  }
  return result;
}

// =========================================================================
// TEMPLATES
// =========================================================================

export type TemplateName = 'crusher_3x3' | 'reactor_5x5' | 'tank_3x3' | 'smelter_3x3';

export function applyTemplate(template: TemplateName): void {
  resetState();

  switch (template) {
    case 'crusher_3x3':
      configureMultiblock({
        name: 'Crusher', structType: 'machine',
        w: 3, h: 3, d: 3,
        ioTypes: ['energy', 'item'],
        capacity: { energy: 64000 },
        processTime: 200, energyPerTick: 32,
        blockId: 213, casingId: 214,
      });
      fillShell();
      placeBlock(1, 1, 0, 'controller');
      placeBlock(2, 1, 1, 'energy_port', 'input');
      placeBlock(0, 1, 1, 'item_port', 'input_output');
      loadGuiPreset('processor');
      break;

    case 'reactor_5x5':
      configureMultiblock({
        name: 'Reactor', structType: 'reactor',
        w: 5, h: 5, d: 5,
        ioTypes: ['energy', 'fluid'],
        capacity: { energy: 256000, fluid: 32000 },
        processTime: 400, energyPerTick: 64,
        blockId: 215, casingId: 216,
      });
      fillShell();
      // Glass top layer center
      for (let x = 1; x <= 3; x++) {
        for (let z = 1; z <= 3; z++) {
          placeBlock(x, 4, z, 'glass');
        }
      }
      placeBlock(2, 2, 0, 'controller');
      placeBlock(4, 2, 2, 'energy_port', 'output');
      placeBlock(0, 2, 2, 'fluid_port', 'input');
      placeBlock(2, 2, 4, 'fluid_port', 'input');
      clearGui();
      addGuiComponent('energy_bar', 10, 16);
      addGuiComponent('fluid_tank', 80, 16);
      addGuiComponent('slot', 55, 34, { slotType: 'input' });
      addGuiComponent('progress_arrow', 79, 35);
      addGuiComponent('slot', 115, 34, { slotType: 'output' });
      break;

    case 'tank_3x3':
      configureMultiblock({
        name: 'Tank', structType: 'tank',
        w: 3, h: 3, d: 3,
        ioTypes: ['fluid'],
        capacity: { fluid: 64000 },
        processTime: 1, energyPerTick: 0,
        blockId: 217, casingId: 218,
      });
      fillShell();
      // Glass front face center
      placeBlock(1, 1, 0, 'glass');
      placeBlock(2, 1, 1, 'fluid_port', 'input_output');
      placeBlock(0, 1, 1, 'fluid_port', 'input_output');
      loadGuiPreset('tank');
      break;

    case 'smelter_3x3':
      configureMultiblock({
        name: 'Smelter', structType: 'machine',
        w: 3, h: 3, d: 3,
        ioTypes: ['energy', 'item'],
        capacity: { energy: 48000 },
        processTime: 160, energyPerTick: 24,
        blockId: 219, casingId: 220,
      });
      fillShell();
      placeBlock(1, 1, 0, 'controller');
      placeBlock(2, 1, 1, 'energy_port', 'input');
      placeBlock(0, 1, 1, 'item_port', 'input_output');
      clearGui();
      addGuiComponent('slot', 45, 25, { slotType: 'input' });
      addGuiComponent('slot', 45, 47, { slotType: 'input' });
      addGuiComponent('flame', 47, 36);
      addGuiComponent('progress_arrow', 73, 35);
      addGuiComponent('big_slot', 107, 30, { slotType: 'output' });
      addGuiComponent('energy_bar', 10, 16);
      break;
  }
}

// =========================================================================
// MODEL
// =========================================================================

export function importBlockbenchModel(json: any, textureName?: string): BlockModel {
  const elements: ModelElement[] = [];
  for (const el of json.elements || []) {
    elements.push({
      name: el.name || `part_${elements.length}`,
      from: [el.from[0], el.from[1], el.from[2]],
      to: [el.to[0], el.to[1], el.to[2]],
    });
  }
  const name = state.name || 'Unnamed';
  const tex = textureName || Object.values(json.textures || {})[0] as string || `${name.toLowerCase()}_texture`;
  const model: BlockModel = { name, elements, textureName: tex };
  state.model = model;
  notifyChange();
  return model;
}

export function getModel(): BlockModel | null {
  return state.model;
}

export function clearModel(): void {
  state.model = null;
  notifyChange();
}

export function getStateSummary(): string {
  const { w, h, d } = state.dimensions;
  const counts: Record<string, number> = {};
  for (const [, block] of state.blocks) {
    counts[block.blockId] = (counts[block.blockId] || 0) + 1;
  }
  const slotInfo = getSlotInfo();
  return [
    `Name: ${state.name} (${state.structType})`,
    `Dimensions: ${w}x${h}x${d}`,
    `IO: ${state.ioTypes.join(', ')}`,
    `Blocks: ${state.blocks.size} total — ${Object.entries(counts).map(([t, n]) => `${t}:${n}`).join(', ') || 'none'}`,
    `GUI: ${state.guiComponents.length} components (${slotInfo.inputs.length} input slots, ${slotInfo.outputs.length} output slots)`,
    `Energy: ${state.capacity.energy} RN, Fluid: ${state.capacity.fluid} mB, Gas: ${state.capacity.gas} mB`,
    `Process: ${state.processTime} ticks @ ${state.energyPerTick} RN/tick`,
    `IDs: block=${state.blockId}, casing=${state.casingId}`,
  ].join('\n');
}
