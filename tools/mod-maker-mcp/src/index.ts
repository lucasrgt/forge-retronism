#!/usr/bin/env node

import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';
import * as fs from 'fs';
import * as path from 'path';

import { execSync } from 'child_process';
// WebSocket import kept for future use but sync is file-based now

import {
  configureMultiblock, placeBlock, removeBlock, fillShell, clearBlocks,
  addGuiComponent, clearGui, loadGuiPreset,
  getStateSummary, resetState, serialize, deserialize, getState,
  setDimensions, updateConfig, removeGuiComponent, updateGuiComponent,
  fillRegion, replaceBlocks, fillLayer, copyLayer,
  mirrorStructure, rotateStructure,
  placeOnFace, placeRing,
  getBlockAt, listBlocksByType,
  applyTemplate,
  importBlockbenchModel, getModel, clearModel,
  setOnChangeListener,
} from './state.js';
import { generateAllFiles } from './codegen.js';
import { blockRegistry } from './types.js';
import type { StructureType, IOType, PortMode, GuiComponentType, SlotType, BlockCategory } from './types.js';
import type { Face, TemplateName } from './state.js';

// ---------------------------------------------------------------------------
// File-based sync for real-time updates to Multiblock Designer UI
// ---------------------------------------------------------------------------
const SYNC_FILE = path.resolve(import.meta.dirname, '..', '..', '..', 'temp', 'mcp_state.json');

function syncStateToFile(): void {
  try {
    const dir = path.dirname(SYNC_FILE);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(SYNC_FILE, JSON.stringify(serialize(), null, 2), 'utf-8');
  } catch (_) {
    // Silently ignore write errors
  }
}

// Register change listener — writes state file after every mutation
setOnChangeListener(syncStateToFile);

const server = new McpServer({
  name: 'retronism-mod-maker',
  version: '1.0.0',
});

// ---------------------------------------------------------------------------
// Zod helpers for dynamic block validation
// ---------------------------------------------------------------------------
const blockIdSchema = (desc?: string) => z.string().superRefine((val, ctx) => {
  if (!blockRegistry.has(val)) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: `Unknown block ID "${val}". Valid: ${blockRegistry.getIds().join(', ')}`,
    });
  }
}).describe(desc || 'Block ID (e.g., casing, controller, energy_port, fluid_port, gas_port, item_port, glass, or any custom registered block)');

// ---------------------------------------------------------------------------
// Tool: create_multiblock
// ---------------------------------------------------------------------------
server.tool(
  'create_multiblock',
  'Create or reconfigure a multiblock structure. Sets name, type, dimensions, IO types, capacities, and IDs.',
  {
    name: z.string().describe('Structure name, PascalCase (e.g. MegaCrusher, NuclearReactor)'),
    structType: z.enum(['machine', 'tank', 'reactor', 'custom']).default('machine'),
    w: z.number().int().min(3).max(9).default(3).describe('Width (odd recommended)'),
    h: z.number().int().min(3).max(9).default(3).describe('Height'),
    d: z.number().int().min(3).max(9).default(3).describe('Depth (odd recommended)'),
    ioTypes: z.array(z.enum(['energy', 'fluid', 'gas', 'item'])).default(['energy', 'item']),
    energyCapacity: z.number().int().default(64000).describe('Max energy in RN'),
    fluidCapacity: z.number().int().default(0).describe('Max fluid in mB'),
    gasCapacity: z.number().int().default(0).describe('Max gas in mB'),
    processTime: z.number().int().default(200).describe('Processing time in ticks'),
    energyPerTick: z.number().int().default(32).describe('Energy consumed per tick'),
    blockId: z.number().int().min(200).max(255).default(213),
    casingId: z.number().int().min(200).max(255).default(214),
    fillWithCasing: z.boolean().default(true).describe('Auto-fill outer shell with casing blocks'),
  },
  async (args) => {
    resetState();
    configureMultiblock({
      name: args.name,
      structType: args.structType as StructureType,
      w: args.w, h: args.h, d: args.d,
      ioTypes: args.ioTypes as IOType[],
      capacity: { energy: args.energyCapacity, fluid: args.fluidCapacity, gas: args.gasCapacity },
      processTime: args.processTime,
      energyPerTick: args.energyPerTick,
      blockId: args.blockId,
      casingId: args.casingId,
    });

    let msg = `Created multiblock "${args.name}" (${args.w}x${args.h}x${args.d} ${args.structType})`;
    if (args.fillWithCasing) {
      const count = fillShell();
      msg += `\nFilled shell with ${count} casing blocks.`;
    }
    msg += '\n\n' + getStateSummary();
    return { content: [{ type: 'text', text: msg }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: place_blocks
// ---------------------------------------------------------------------------
server.tool(
  'place_blocks',
  'Place one or more blocks in the multiblock structure. Use for setting controller, ports, glass, etc.',
  {
    blocks: z.array(z.object({
      x: z.number().int(),
      y: z.number().int(),
      z: z.number().int(),
      type: blockIdSchema('Block ID to place'),
      mode: z.enum(['input', 'output', 'input_output']).default('input_output').describe('Port mode (only for port types)'),
    })).describe('Array of blocks to place'),
  },
  async (args) => {
    let placed = 0;
    let failed = 0;
    for (const b of args.blocks) {
      if (placeBlock(b.x, b.y, b.z, b.type, b.mode as PortMode)) {
        placed++;
      } else {
        failed++;
      }
    }
    let msg = `Placed ${placed} blocks.`;
    if (failed > 0) msg += ` ${failed} failed (out of bounds).`;
    msg += '\n\n' + getStateSummary();
    return { content: [{ type: 'text', text: msg }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: fill_shell
// ---------------------------------------------------------------------------
server.tool(
  'fill_shell',
  'Fill the outer shell of the structure with casing blocks (does not overwrite existing blocks).',
  {},
  async () => {
    const count = fillShell();
    return { content: [{ type: 'text', text: `Filled ${count} positions with casing.\n\n${getStateSummary()}` }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: clear_blocks
// ---------------------------------------------------------------------------
server.tool(
  'clear_blocks',
  'Remove all blocks from the structure.',
  {},
  async () => {
    clearBlocks();
    return { content: [{ type: 'text', text: 'All blocks cleared.\n\n' + getStateSummary() }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: setup_gui
// ---------------------------------------------------------------------------
server.tool(
  'setup_gui',
  'Set up the machine GUI. Can load a preset and/or add individual components. Presets: processor, dual_input, single_slot, tank.',
  {
    preset: z.enum(['processor', 'dual_input', 'single_slot', 'tank', 'none']).default('none').describe('Load a preset layout first'),
    components: z.array(z.object({
      type: z.enum(['slot', 'big_slot', 'energy_bar', 'progress_arrow', 'flame', 'fluid_tank', 'gas_tank', 'separator']),
      x: z.number().int().min(0).max(175),
      y: z.number().int().min(0).max(165),
      w: z.number().int().optional().describe('Width override (for resizable components)'),
      h: z.number().int().optional().describe('Height override (for resizable components)'),
      slotType: z.enum(['input', 'output', 'fuel']).optional().describe('Slot role (for slot/big_slot)'),
    })).default([]).describe('Additional components to add after preset'),
  },
  async (args) => {
    if (args.preset !== 'none') {
      loadGuiPreset(args.preset);
    }
    for (const c of args.components) {
      addGuiComponent(c.type as GuiComponentType, c.x, c.y, {
        w: c.w, h: c.h, slotType: c.slotType as SlotType | undefined,
      });
    }
    const state = getState();
    const comps = state.guiComponents.map((c, i) =>
      `  [${i}] ${c.type} at (${c.x},${c.y}) ${c.w}x${c.h}${c.slotType ? ` [${c.slotType}]` : ''}`
    ).join('\n');
    return { content: [{ type: 'text', text: `GUI has ${state.guiComponents.length} components:\n${comps}` }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: get_state
// ---------------------------------------------------------------------------
server.tool(
  'get_state',
  'View the current multiblock state: blocks, GUI components, config.',
  {},
  async () => {
    const state = getState();
    const { w, h, d } = state.dimensions;

    // Render layers as text grid
    let layerView = '';
    for (let y = 0; y < h; y++) {
      layerView += `Layer ${y}:\n`;
      for (let z = 0; z < d; z++) {
        let row = '  ';
        for (let x = 0; x < w; x++) {
          const block = state.blocks.get(`${x},${y},${z}`);
          row += block ? (blockRegistry.get(block.blockId)?.char || '?') : '.';
        }
        layerView += row + '\n';
      }
    }

    const comps = state.guiComponents.map((c, i) =>
      `  [${i}] ${c.type} at (${c.x},${c.y}) ${c.w}x${c.h}${c.slotType ? ` [${c.slotType}]` : ''}`
    ).join('\n');

    return {
      content: [{
        type: 'text',
        text: getStateSummary() + '\n\nStructure:\n' + layerView + '\nGUI Components:\n' + (comps || '  (none)'),
      }],
    };
  },
);

// ---------------------------------------------------------------------------
// Tool: generate_code
// ---------------------------------------------------------------------------
server.tool(
  'generate_code',
  'Generate all Java files, JSON definition, and GUI builder script for the current multiblock. Returns file listing with paths.',
  {
    showContent: z.boolean().default(false).describe('Include file contents in output'),
  },
  async (args) => {
    const files = generateAllFiles();
    let text = `Generated ${files.length} files:\n\n`;
    for (const f of files) {
      text += `--- ${f.relativePath} ---\n`;
      if (args.showContent) {
        text += f.content + '\n\n';
      }
    }
    return { content: [{ type: 'text', text }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: export_to_mod
// ---------------------------------------------------------------------------
server.tool(
  'export_to_mod',
  'Generate and write all files to the RetroNism mod project directory. This writes Java source, JSON, and GUI script.',
  {},
  async () => {
    const files = generateAllFiles();
    // Find project root (tools/mod-maker-mcp/../../ = project root)
    const projectRoot = path.resolve(import.meta.dirname, '..', '..', '..');
    const results: string[] = [];

    for (const f of files) {
      const fullPath = path.join(projectRoot, f.relativePath);
      try {
        fs.mkdirSync(path.dirname(fullPath), { recursive: true });
        fs.writeFileSync(fullPath, f.content, 'utf-8');
        results.push(`OK  ${f.relativePath}`);
      } catch (err: any) {
        results.push(`ERR ${f.relativePath}: ${err.message}`);
      }
    }

    return { content: [{ type: 'text', text: `Exported ${files.length} files:\n\n${results.join('\n')}` }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: remove_blocks
// ---------------------------------------------------------------------------
server.tool(
  'remove_blocks',
  'Remove one or more blocks from the structure by position.',
  {
    positions: z.array(z.object({
      x: z.number().int(),
      y: z.number().int(),
      z: z.number().int(),
    })).describe('Array of {x,y,z} positions to remove'),
  },
  async (args) => {
    let removed = 0;
    for (const p of args.positions) {
      if (removeBlock(p.x, p.y, p.z)) removed++;
    }
    return { content: [{ type: 'text', text: `Removed ${removed} blocks.\n\n${getStateSummary()}` }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: update_config
// ---------------------------------------------------------------------------
server.tool(
  'update_config',
  'Update multiblock config without resetting. Change name, type, IO types, capacities, IDs, process time, etc.',
  {
    name: z.string().optional().describe('Structure name (PascalCase)'),
    structType: z.enum(['machine', 'tank', 'reactor', 'custom']).optional(),
    ioTypes: z.array(z.enum(['energy', 'fluid', 'gas', 'item'])).optional(),
    energyCapacity: z.number().int().optional().describe('Max energy in RN'),
    fluidCapacity: z.number().int().optional().describe('Max fluid in mB'),
    gasCapacity: z.number().int().optional().describe('Max gas in mB'),
    processTime: z.number().int().optional().describe('Processing time in ticks'),
    energyPerTick: z.number().int().optional().describe('Energy consumed per tick'),
    blockId: z.number().int().min(200).max(255).optional(),
    casingId: z.number().int().min(200).max(255).optional(),
  },
  async (args) => {
    updateConfig({
      name: args.name,
      structType: args.structType as any,
      ioTypes: args.ioTypes as any,
      energyCapacity: args.energyCapacity,
      fluidCapacity: args.fluidCapacity,
      gasCapacity: args.gasCapacity,
      processTime: args.processTime,
      energyPerTick: args.energyPerTick,
      blockId: args.blockId,
      casingId: args.casingId,
    });
    return { content: [{ type: 'text', text: `Config updated.\n\n${getStateSummary()}` }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: set_dimensions
// ---------------------------------------------------------------------------
server.tool(
  'set_dimensions',
  'Resize the multiblock structure. Blocks outside new bounds are removed.',
  {
    w: z.number().int().min(3).max(9).describe('Width'),
    h: z.number().int().min(3).max(9).describe('Height'),
    d: z.number().int().min(3).max(9).describe('Depth'),
  },
  async (args) => {
    const removed = setDimensions(args.w, args.h, args.d);
    let msg = `Dimensions set to ${args.w}x${args.h}x${args.d}.`;
    if (removed > 0) msg += ` Removed ${removed} out-of-bounds blocks.`;
    msg += '\n\n' + getStateSummary();
    return { content: [{ type: 'text', text: msg }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: clear_gui
// ---------------------------------------------------------------------------
server.tool(
  'clear_gui',
  'Remove all GUI components.',
  {},
  async () => {
    clearGui();
    return { content: [{ type: 'text', text: 'GUI cleared.\n\n' + getStateSummary() }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: remove_gui_component
// ---------------------------------------------------------------------------
server.tool(
  'remove_gui_component',
  'Remove a GUI component by its index. Use get_state to see component indices.',
  {
    index: z.number().int().min(0).describe('Component index to remove'),
  },
  async (args) => {
    if (removeGuiComponent(args.index)) {
      const state = getState();
      const comps = state.guiComponents.map((c, i) =>
        `  [${i}] ${c.type} at (${c.x},${c.y}) ${c.w}x${c.h}${c.slotType ? ` [${c.slotType}]` : ''}`
      ).join('\n');
      return { content: [{ type: 'text', text: `Removed component ${args.index}.\n\nGUI has ${state.guiComponents.length} components:\n${comps || '  (none)'}` }] };
    }
    return { content: [{ type: 'text', text: `Invalid index ${args.index}.` }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: update_gui_component
// ---------------------------------------------------------------------------
server.tool(
  'update_gui_component',
  'Move, resize, or change a GUI component by index. Use get_state to see indices.',
  {
    index: z.number().int().min(0).describe('Component index to update'),
    x: z.number().int().min(0).max(175).optional().describe('New X position'),
    y: z.number().int().min(0).max(165).optional().describe('New Y position'),
    w: z.number().int().optional().describe('New width (resizable components only)'),
    h: z.number().int().optional().describe('New height (resizable components only)'),
    slotType: z.enum(['input', 'output', 'fuel']).optional().describe('Slot role (slot/big_slot only)'),
  },
  async (args) => {
    if (updateGuiComponent(args.index, {
      x: args.x, y: args.y, w: args.w, h: args.h,
      slotType: args.slotType as any,
    })) {
      const state = getState();
      const comp = state.guiComponents[args.index];
      return { content: [{ type: 'text', text: `Updated component [${args.index}]: ${comp.type} at (${comp.x},${comp.y}) ${comp.w}x${comp.h}${comp.slotType ? ` [${comp.slotType}]` : ''}` }] };
    }
    return { content: [{ type: 'text', text: `Invalid index ${args.index}.` }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: import_json
// ---------------------------------------------------------------------------
server.tool(
  'import_json',
  'Load a multiblock definition from a JSON string or file path. Replaces current state entirely.',
  {
    json: z.string().optional().describe('JSON string of a serialized multiblock'),
    filePath: z.string().optional().describe('Path to a .json file to load'),
  },
  async (args) => {
    let raw: string;
    if (args.filePath) {
      try {
        raw = fs.readFileSync(args.filePath, 'utf-8');
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Error reading file: ${err.message}` }] };
      }
    } else if (args.json) {
      raw = args.json;
    } else {
      return { content: [{ type: 'text', text: 'Provide either json or filePath.' }] };
    }
    try {
      const data = JSON.parse(raw);
      deserialize(data);
      return { content: [{ type: 'text', text: `Imported "${data.name}".\n\n${getStateSummary()}` }] };
    } catch (err: any) {
      return { content: [{ type: 'text', text: `Error parsing JSON: ${err.message}` }] };
    }
  },
);

// ---------------------------------------------------------------------------
// Tool: export_json
// ---------------------------------------------------------------------------
server.tool(
  'export_json',
  'Export the current multiblock state as a JSON string. Optionally save to a file.',
  {
    filePath: z.string().optional().describe('If provided, save JSON to this file path'),
  },
  async (args) => {
    const data = serialize();
    const jsonStr = JSON.stringify(data, null, 2);
    if (args.filePath) {
      try {
        fs.mkdirSync(path.dirname(args.filePath), { recursive: true });
        fs.writeFileSync(args.filePath, jsonStr, 'utf-8');
        return { content: [{ type: 'text', text: `Saved to ${args.filePath}\n\n${jsonStr}` }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Error writing file: ${err.message}\n\nJSON:\n${jsonStr}` }] };
      }
    }
    return { content: [{ type: 'text', text: jsonStr }] };
  },
);

// ===========================================================================
// BATCH OPERATIONS
// ===========================================================================

// Tool: fill_region
server.tool(
  'fill_region',
  'Fill a rectangular region with a block type. Coords are inclusive. Set overwrite=true to replace existing blocks.',
  {
    x1: z.number().int(), y1: z.number().int(), z1: z.number().int(),
    x2: z.number().int(), y2: z.number().int(), z2: z.number().int(),
    type: blockIdSchema('Block ID to fill with'),
    mode: z.enum(['input', 'output', 'input_output']).default('input_output'),
    overwrite: z.boolean().default(false),
  },
  async (args) => {
    const count = fillRegion(args.x1, args.y1, args.z1, args.x2, args.y2, args.z2, args.type, args.mode as PortMode, args.overwrite);
    return { content: [{ type: 'text', text: `Filled ${count} positions.\n\n${getStateSummary()}` }] };
  },
);

// Tool: replace_blocks
server.tool(
  'replace_blocks',
  'Replace all blocks of one type with another type throughout the structure.',
  {
    fromType: blockIdSchema('Block ID to replace'),
    toType: blockIdSchema('Block ID to replace with'),
    mode: z.enum(['input', 'output', 'input_output']).optional().describe('New port mode (optional)'),
  },
  async (args) => {
    const count = replaceBlocks(args.fromType, args.toType, args.mode as PortMode | undefined);
    return { content: [{ type: 'text', text: `Replaced ${count} blocks (${args.fromType} -> ${args.toType}).\n\n${getStateSummary()}` }] };
  },
);

// Tool: fill_layer
server.tool(
  'fill_layer',
  'Fill an entire layer at Y height. Mode "shell" fills only the border ring, "all" fills every position.',
  {
    y: z.number().int().min(0).describe('Layer Y index'),
    fillMode: z.enum(['shell', 'all']).default('shell'),
    type: blockIdSchema('Block ID to fill with').default('casing'),
    mode: z.enum(['input', 'output', 'input_output']).default('input_output'),
  },
  async (args) => {
    const count = fillLayer(args.y, args.fillMode, args.type, args.mode as PortMode);
    return { content: [{ type: 'text', text: `Filled ${count} positions on layer Y=${args.y} (${args.fillMode}).\n\n${getStateSummary()}` }] };
  },
);

// ===========================================================================
// LAYER OPERATIONS
// ===========================================================================

// Tool: copy_layer
server.tool(
  'copy_layer',
  'Copy all blocks from one layer to another. Set overwrite=true to replace existing blocks on the destination layer.',
  {
    srcY: z.number().int().min(0).describe('Source layer Y'),
    dstY: z.number().int().min(0).describe('Destination layer Y'),
    overwrite: z.boolean().default(false),
  },
  async (args) => {
    const count = copyLayer(args.srcY, args.dstY, args.overwrite);
    return { content: [{ type: 'text', text: `Copied ${count} blocks from Y=${args.srcY} to Y=${args.dstY}.\n\n${getStateSummary()}` }] };
  },
);

// Tool: mirror_structure
server.tool(
  'mirror_structure',
  'Mirror the entire structure along an axis. "x" flips left/right, "z" flips front/back.',
  {
    axis: z.enum(['x', 'z']).describe('Axis to mirror along'),
  },
  async (args) => {
    const count = mirrorStructure(args.axis);
    return { content: [{ type: 'text', text: `Mirrored ${count} blocks along ${args.axis} axis.\n\n${getStateSummary()}` }] };
  },
);

// Tool: rotate_structure
server.tool(
  'rotate_structure',
  'Rotate the entire structure 90 degrees clockwise (viewed from above). Swaps W and D dimensions if needed.',
  {},
  async () => {
    const result = rotateStructure();
    return { content: [{ type: 'text', text: `Rotated ${result.count} blocks 90° CW. New dimensions: ${result.w}x${getState().dimensions.h}x${result.d}.\n\n${getStateSummary()}` }] };
  },
);

// ===========================================================================
// SMART PLACEMENT
// ===========================================================================

// Tool: place_on_face
server.tool(
  'place_on_face',
  'Replace blocks on a face with another block type. By default only replaces casing; set replace=true to replace any block. Places toward center first. count=0 replaces ALL eligible blocks on that face.',
  {
    face: z.enum(['north', 'south', 'east', 'west', 'top', 'bottom']).describe('Face to place on (north=z0, south=zMax, west=x0, east=xMax)'),
    type: blockIdSchema('Block ID to place (any registered block: casing, glass, energy_port, fluid_port, gas_port, item_port, controller, or custom)'),
    mode: z.enum(['input', 'output', 'input_output']).default('input_output').describe('Port mode (only relevant for port types)'),
    count: z.number().int().min(0).default(0).describe('Number of blocks to place (0 = all eligible on face)'),
    replace: z.boolean().default(false).describe('If true, replace ANY block on the face (not just casing)'),
  },
  async (args) => {
    const placed = placeOnFace(args.face as Face, args.type, args.mode as PortMode, args.count, args.replace);
    return { content: [{ type: 'text', text: `Placed ${placed} ${args.type} on ${args.face} face.\n\n${getStateSummary()}` }] };
  },
);

// Tool: place_ring
server.tool(
  'place_ring',
  'Place a ring of blocks around the border of a specific layer Y (overwrites existing).',
  {
    y: z.number().int().min(0).describe('Layer Y index'),
    type: blockIdSchema('Block ID for the ring').default('casing'),
    mode: z.enum(['input', 'output', 'input_output']).default('input_output'),
  },
  async (args) => {
    const count = placeRing(args.y, args.type, args.mode as PortMode);
    return { content: [{ type: 'text', text: `Placed ring of ${count} ${args.type} blocks on Y=${args.y}.\n\n${getStateSummary()}` }] };
  },
);

// ===========================================================================
// QUERY
// ===========================================================================

// Tool: get_block_at
server.tool(
  'get_block_at',
  'Get the block type and mode at a specific position. Returns null if empty.',
  {
    x: z.number().int(), y: z.number().int(), z: z.number().int(),
  },
  async (args) => {
    const block = getBlockAt(args.x, args.y, args.z);
    if (block) {
      return { content: [{ type: 'text', text: `Block at (${args.x},${args.y},${args.z}): ${block.blockId} [${block.mode}]` }] };
    }
    return { content: [{ type: 'text', text: `No block at (${args.x},${args.y},${args.z}) (air/empty)` }] };
  },
);

// Tool: list_blocks_by_type
server.tool(
  'list_blocks_by_type',
  'List all positions of blocks with a specific type.',
  {
    type: blockIdSchema('Block ID to search for'),
  },
  async (args) => {
    const blocks = listBlocksByType(args.type);
    if (blocks.length === 0) {
      return { content: [{ type: 'text', text: `No ${args.type} blocks found.` }] };
    }
    const list = blocks.map(b => `  (${b.x},${b.y},${b.z}) [${b.mode}]`).join('\n');
    return { content: [{ type: 'text', text: `Found ${blocks.length} ${args.type} blocks:\n${list}` }] };
  },
);

// ===========================================================================
// TEMPLATES
// ===========================================================================

// Tool: apply_template
server.tool(
  'apply_template',
  'Apply a predefined multiblock template. Resets current state. Templates: crusher_3x3, reactor_5x5, tank_3x3, smelter_3x3.',
  {
    template: z.enum(['crusher_3x3', 'reactor_5x5', 'tank_3x3', 'smelter_3x3']),
  },
  async (args) => {
    applyTemplate(args.template as TemplateName);
    return { content: [{ type: 'text', text: `Applied template "${args.template}".\n\n${getStateSummary()}` }] };
  },
);

// ===========================================================================
// BLOCK REGISTRY
// ===========================================================================

// Tool: register_block
server.tool(
  'register_block',
  'Register a new custom block type for use in multiblock structures. The block becomes available in all placement tools immediately.',
  {
    id: z.string().regex(/^[a-z][a-z0-9_]*$/).describe('Unique block ID in snake_case (e.g., steel_casing, hv_energy_port)'),
    category: z.enum(['casing', 'controller', 'port', 'glass', 'custom']).describe('Block category — determines behavior in structure validation and codegen'),
    label: z.string().describe('Display label (e.g., "Steel Casing")'),
    color: z.number().int().describe('Hex color for 3D preview (e.g., 0xAABBCC)'),
    char: z.string().max(1).optional().describe('Single char for serialization (auto-assigned if omitted)'),
    portType: z.enum(['energy', 'fluid', 'gas', 'item']).optional().describe('Port IO type (required for category "port")'),
    tier: z.number().int().optional().describe('Optional tier number'),
  },
  async (args) => {
    const char = args.char || blockRegistry.nextAvailableChar();
    if (!char) {
      return { content: [{ type: 'text', text: 'Error: no available characters for serialization.' }] };
    }
    try {
      blockRegistry.register({
        id: args.id,
        category: args.category as BlockCategory,
        label: args.label,
        color: args.color,
        char,
        portType: args.portType as any,
        tier: args.tier,
        builtIn: false,
      });
      const all = blockRegistry.getAll();
      return { content: [{ type: 'text', text: `Registered block "${args.id}" (${args.category}, char='${char}').\n\nRegistry now has ${all.length} block types: ${all.map(b => b.id).join(', ')}` }] };
    } catch (err: any) {
      return { content: [{ type: 'text', text: `Error: ${err.message}` }] };
    }
  },
);

// Tool: unregister_block
server.tool(
  'unregister_block',
  'Remove a custom block type from the registry. Built-in blocks cannot be removed.',
  {
    id: z.string().describe('Block ID to remove'),
  },
  async (args) => {
    if (blockRegistry.unregister(args.id)) {
      return { content: [{ type: 'text', text: `Unregistered block "${args.id}".\n\nRegistry: ${blockRegistry.getIds().join(', ')}` }] };
    }
    const def = blockRegistry.get(args.id);
    if (def?.builtIn) {
      return { content: [{ type: 'text', text: `Cannot remove built-in block "${args.id}".` }] };
    }
    return { content: [{ type: 'text', text: `Block "${args.id}" not found in registry.` }] };
  },
);

// Tool: list_block_registry
server.tool(
  'list_block_registry',
  'List all registered block types with their properties (ID, category, label, color, char, portType, tier).',
  {},
  async () => {
    const all = blockRegistry.getAll();
    const lines = all.map(b => {
      let line = `  ${b.id} — ${b.label} [${b.category}] char='${b.char}' color=0x${b.color.toString(16).padStart(6, '0')}`;
      if (b.portType) line += ` portType=${b.portType}`;
      if (b.tier !== undefined) line += ` tier=${b.tier}`;
      if (b.builtIn) line += ' (built-in)';
      return line;
    });
    return { content: [{ type: 'text', text: `Block Registry (${all.length} types):\n${lines.join('\n')}` }] };
  },
);

// ===========================================================================
// MODEL
// ===========================================================================

// Tool: import_model
server.tool(
  'import_model',
  'Import a Blockbench JSON model and link it to the current multiblock. The model will be included in exports.',
  {
    filePath: z.string().optional().describe('Path to a Blockbench .json model file'),
    json: z.string().optional().describe('Blockbench JSON as a string'),
    textureName: z.string().optional().describe('Override texture name (default: auto-detected from model)'),
  },
  async (args) => {
    let raw: string;
    if (args.filePath) {
      try {
        raw = fs.readFileSync(args.filePath, 'utf-8');
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Error reading file: ${err.message}` }] };
      }
    } else if (args.json) {
      raw = args.json;
    } else {
      return { content: [{ type: 'text', text: 'Provide either filePath or json.' }] };
    }
    try {
      const data = JSON.parse(raw);
      const model = importBlockbenchModel(data, args.textureName);
      return { content: [{ type: 'text', text: `Imported model "${model.name}" with ${model.elements.length} elements.\nTexture: ${model.textureName}\nElements:\n${model.elements.map(e => `  ${e.name}: [${e.from}] → [${e.to}]`).join('\n')}` }] };
    } catch (err: any) {
      return { content: [{ type: 'text', text: `Error parsing model: ${err.message}` }] };
    }
  },
);

// Tool: clear_model
server.tool(
  'clear_model',
  'Remove the linked 3D model from the current multiblock.',
  {},
  async () => {
    clearModel();
    return { content: [{ type: 'text', text: 'Model cleared.' }] };
  },
);

// Tool: export_model_context
server.tool(
  'export_model_context',
  'Export a markdown document with XML metadata describing the current multiblock. Designed to be consumed by Claude as context before using Blockbench MCP to create a 3D model.',
  {
    filePath: z.string().optional().describe('If provided, save the .md file to this path'),
  },
  async (args) => {
    const state = getState();
    const { w, h, d } = state.dimensions;

    // Collect block counts and port info
    const counts: Record<string, number> = {};
    const portDetails: { blockId: string; pos: string; mode: PortMode; portType?: string }[] = [];
    for (const [key, block] of state.blocks) {
      counts[block.blockId] = (counts[block.blockId] || 0) + 1;
      const def = blockRegistry.get(block.blockId);
      if (def?.category === 'port') {
        portDetails.push({ blockId: block.blockId, pos: key, mode: block.mode, portType: def.portType });
      }
    }

    // Collect block palette with colors
    const usedBlocks = new Set<string>();
    for (const [, block] of state.blocks) usedBlocks.add(block.blockId);
    const palette = [...usedBlocks].map(id => {
      const def = blockRegistry.get(id);
      return def ? `  <block id="${id}" category="${def.category}" color="#${def.color.toString(16).padStart(6, '0')}" label="${def.label}"${def.portType ? ` portType="${def.portType}"` : ''} count="${counts[id] || 0}" />` : '';
    }).filter(Boolean);

    // Structure layers as visual grid
    let layerGrid = '';
    for (let y = 0; y < h; y++) {
      layerGrid += `  <layer y="${y}">\n`;
      for (let z = 0; z < d; z++) {
        let row = '    ';
        for (let x = 0; x < w; x++) {
          const block = state.blocks.get(`${x},${y},${z}`);
          row += block ? (blockRegistry.get(block.blockId)?.char || '?') : '.';
        }
        layerGrid += row + '\n';
      }
      layerGrid += '  </layer>\n';
    }

    // Port details
    const portXml = portDetails.map(p =>
      `  <port type="${p.portType}" blockId="${p.blockId}" position="${p.pos}" mode="${p.mode}" />`
    ).join('\n');

    // Model info
    const modelInfo = state.model
      ? `<model name="${state.model.name}" texture="${state.model.textureName}" elements="${state.model.elements.length}" />`
      : '<model>none linked</model>';

    const md = `# ${state.name} — Multiblock Model Context

> Auto-generated metadata for Blockbench MCP model creation.

<machine>
  <name>${state.name}</name>
  <type>${state.structType}</type>
  <dimensions width="${w}" height="${h}" depth="${d}" />
  <blockSize>16</blockSize>
  <modelSize width="${w * 16}" height="${h * 16}" depth="${d * 16}" unit="pixels" />
</machine>

<io types="${state.ioTypes.join(', ')}">
  <energy capacity="${state.capacity.energy}" perTick="${state.energyPerTick}" />
  <fluid capacity="${state.capacity.fluid}" />
  <gas capacity="${state.capacity.gas}" />
  <processTime ticks="${state.processTime}" />
</io>

<palette>
${palette.join('\n')}
</palette>

<structure totalBlocks="${state.blocks.size}">
${layerGrid}</structure>

<ports count="${portDetails.length}">
${portXml}
</ports>

<gui components="${state.guiComponents.length}">
${state.guiComponents.map((c, i) => `  <component index="${i}" type="${c.type}" x="${c.x}" y="${c.y}" w="${c.w}" h="${c.h}"${c.slotType ? ` slotType="${c.slotType}"` : ''} />`).join('\n')}
</gui>

${modelInfo}

## Notes for Blockbench

- The controller block is the visual centerpiece — give it a distinct front face
- Port blocks should have subtle visual indicators (colored dots, arrows, or vents)
- Glass blocks should be transparent/translucent
- The model uses \`setBlockBounds\` + \`renderStandardBlock\` for world rendering (axis-aligned boxes only)
- Inventory rendering uses Tessellator with 6-face manual draw per box part
- Texture is a single 16x16 atlas referenced by all faces
- Each element maps to a \`float[] {fromX, fromY, fromZ, toX, toY, toZ}\` in the Java PARTS array
`;

    if (args.filePath) {
      try {
        fs.mkdirSync(path.dirname(args.filePath), { recursive: true });
        fs.writeFileSync(args.filePath, md, 'utf-8');
        return { content: [{ type: 'text', text: `Saved model context to ${args.filePath}\n\n${md}` }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Error writing file: ${err.message}\n\n${md}` }] };
      }
    }
    return { content: [{ type: 'text', text: md }] };
  },
);

// ===========================================================================
// BUILD PIPELINE
// ===========================================================================

// Tool: build_and_export
server.tool(
  'build_and_export',
  'Full build pipeline: generate all code, write to mod project, and run gui_builder.py to create the GUI texture PNG.',
  {},
  async () => {
    const files = generateAllFiles();
    const projectRoot = path.resolve(import.meta.dirname, '..', '..', '..');
    const results: string[] = [];

    for (const f of files) {
      const fullPath = path.join(projectRoot, f.relativePath);
      try {
        fs.mkdirSync(path.dirname(fullPath), { recursive: true });
        fs.writeFileSync(fullPath, f.content, 'utf-8');
        results.push(`OK  ${f.relativePath}`);
      } catch (err: any) {
        results.push(`ERR ${f.relativePath}: ${err.message}`);
      }
    }

    // Run GUI builder script if it exists
    const guiScript = files.find(f => f.name.startsWith('build_gui_'));
    let guiResult = '';
    if (guiScript) {
      const scriptPath = path.join(projectRoot, guiScript.relativePath);
      try {
        const output = execSync(`python "${scriptPath}"`, { cwd: projectRoot, timeout: 30000 }).toString().trim();
        guiResult = `\nGUI texture: ${output}`;
      } catch (err: any) {
        guiResult = `\nGUI texture generation failed: ${err.message}`;
      }
    }

    return { content: [{ type: 'text', text: `Build complete — ${files.length} files exported:\n\n${results.join('\n')}${guiResult}` }] };
  },
);

// ---------------------------------------------------------------------------
// Start server
// ---------------------------------------------------------------------------
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch(console.error);
