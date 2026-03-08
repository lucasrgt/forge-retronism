#!/usr/bin/env node

import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';
import * as fs from 'fs';
import * as path from 'path';

import { execSync } from 'child_process';
import WebSocket from 'ws';

import {
  configureMultiblock, placeBlock, removeBlock, fillShell, clearBlocks,
  addGuiComponent, clearGui, loadGuiPreset,
  getStateSummary, resetState, serialize, deserialize, getState,
  setDimensions, updateConfig, removeGuiComponent, updateGuiComponent,
  fillRegion, replaceBlocks, fillLayer, copyLayer,
  mirrorStructure, rotateStructure,
  placeOnFace, placeRing,
  getBlockAt, listBlocksByType,
  setPortType, setController,
  applyTemplate,
  importBlockbenchModel, getModel, clearModel,
  setOnChangeListener,
} from './state.js';
import { generateAllFiles } from './codegen.js';
import { blockRegistry } from './types.js';
import type { StructureType, IOType, PortMode, GuiComponentType, SlotType, IoMode, BlockCategory } from './types.js';
import type { Face, TemplateName } from './state.js';

// ---------------------------------------------------------------------------
// WebSocket client — connects to Mod Maker Electron app (WS server on :19400)
// ---------------------------------------------------------------------------
const WS_URL = 'ws://127.0.0.1:19400';
let wsConnection: WebSocket | null = null;
let wsReconnectTimer: ReturnType<typeof setTimeout> | null = null;

function wsSend(msg: object): void {
  if (wsConnection && wsConnection.readyState === WebSocket.OPEN) {
    wsConnection.send(JSON.stringify(msg));
  }
}

function isElectronConnected(): boolean {
  return wsConnection !== null && wsConnection.readyState === WebSocket.OPEN;
}

function requireElectron(): string | null {
  if (!isElectronConnected()) {
    return 'Error: Mod Maker app is not open. Please open the Electron app first (cd tools/mod-maker && npm run dev), then try again.';
  }
  return null;
}

function syncStateViaWs(): void {
  wsSend({ type: 'state', payload: serialize() });
}

// Also keep file-based sync as fallback
const SYNC_FILE = path.resolve(import.meta.dirname, '..', '..', '..', 'temp', 'mcp_state.json');

function syncStateToFile(): void {
  try {
    const dir = path.dirname(SYNC_FILE);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(SYNC_FILE, JSON.stringify(serialize(), null, 2), 'utf-8');
  } catch (_) {}
}

// Register change listener — send via WebSocket + write file
setOnChangeListener(() => {
  syncStateViaWs();
  syncStateToFile();
});

function connectToModMaker(): void {
  if (wsConnection) {
    try { wsConnection.close(); } catch (_) {}
    wsConnection = null;
  }

  try {
    const ws = new WebSocket(WS_URL);

    ws.on('open', () => {
      wsConnection = ws;
      // Send current state on connect so the UI is immediately in sync
      wsSend({ type: 'state', payload: serialize() });
    });

    ws.on('close', () => {
      wsConnection = null;
      // Auto-reconnect every 3s — the Electron app may not be open yet
      if (!wsReconnectTimer) {
        wsReconnectTimer = setTimeout(() => {
          wsReconnectTimer = null;
          connectToModMaker();
        }, 3000);
      }
    });

    ws.on('error', () => {
      // 'close' event will fire and handle reconnect
    });
  } catch (_) {
    if (!wsReconnectTimer) {
      wsReconnectTimer = setTimeout(() => {
        wsReconnectTimer = null;
        connectToModMaker();
      }, 3000);
    }
  }
}

connectToModMaker();

const server = new McpServer({
  name: 'retronism-mod-maker',
  version: '1.0.0',
});

// Read-only tools that don't require the Electron app to be open
const READ_ONLY_TOOLS = new Set([
  'get_state', 'get_block_at', 'list_blocks_by_type', 'list_block_registry',
  'list_projects', 'export_json', 'export_model_context',
]);

// Wrap server.tool to auto-inject Electron connection guard on mutating tools
const originalTool = server.tool.bind(server);
server.tool = function (...toolArgs: any[]) {
  // server.tool(name, desc, schema, handler) or server.tool(name, desc, handler)
  const handlerIdx = toolArgs.length - 1;
  const toolName = toolArgs[0] as string;
  const originalHandler = toolArgs[handlerIdx];

  if (!READ_ONLY_TOOLS.has(toolName)) {
    toolArgs[handlerIdx] = async (...handlerArgs: any[]) => {
      const guard = requireElectron();
      if (guard) return { content: [{ type: 'text' as const, text: guard }] };
      return originalHandler(...handlerArgs);
    };
  }
  return (originalTool as any)(...toolArgs);
} as any;

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
}).describe(desc || 'Block ID (e.g., casing, controller, glass, iron_block, stone, or any registered block)');

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
    defaultShellBlock: z.string().default('iron_block').describe('Block ID to use for shell filling (e.g. iron_block, stone, cobblestone)'),
    fillShell: z.boolean().default(true).describe('Auto-fill outer shell with default shell block'),
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
      defaultShellBlock: args.defaultShellBlock,
    });

    // Auto-register a controller block for this multiblock if it doesn't exist yet
    const ctrlId = args.name.replace(/([A-Z])/g, '_$1').toLowerCase().replace(/^_/, '') + '_ctrl';
    if (!blockRegistry.has(ctrlId)) {
      const char = blockRegistry.nextAvailableChar();
      if (char) {
        try {
          blockRegistry.register({
            id: ctrlId,
            category: 'controller' as BlockCategory,
            label: `${args.name} Controller`,
            color: 0x6a8a6a,
            char,
            builtIn: false,
            mcId: args.blockId,
            terrainIndex: 45,
          });
        } catch (_) { /* ignore if already exists */ }
      }
    }

    let msg = `Created multiblock "${args.name}" (${args.w}x${args.h}x${args.d} ${args.structType})`;
    if (args.fillShell) {
      const count = fillShell();
      msg += `\nFilled shell with ${count} ${args.defaultShellBlock} blocks.`;
    }
    msg += `\nController block "${ctrlId}" registered in block palette.`;
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
  'Fill the outer shell of the structure with the default shell block (does not overwrite existing blocks).',
  {},
  async () => {
    const count = fillShell();
    return { content: [{ type: 'text', text: `Filled ${count} positions with default shell block.\n\n${getStateSummary()}` }] };
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
  'Set up the machine GUI. Load a preset based on real mod machines, then optionally add/modify components. Presets: processor (crusher-style), triple_processor (mega crusher 3-lane), dual_input (2 in → 1 big out), generator (fuel + flame), pump (fluid tank + bucket), fluid_to_gas (electrolysis-style), single_slot, tank, fluid_processor (fluid in → items → fluid out).',
  {
    preset: z.enum(['processor', 'triple_processor', 'dual_input', 'generator', 'pump', 'fluid_to_gas', 'single_slot', 'tank', 'fluid_processor', 'none']).default('none').describe('Load a preset layout based on real mod machines'),
    components: z.array(z.object({
      type: z.enum(['slot', 'big_slot', 'energy_bar', 'progress_arrow', 'flame', 'fluid_tank', 'gas_tank', 'fluid_tank_small', 'gas_tank_small', 'separator']),
      x: z.number().int().min(0).max(175),
      y: z.number().int().min(0).max(165),
      w: z.number().int().optional().describe('Width override (for resizable components)'),
      h: z.number().int().optional().describe('Height override (for resizable components)'),
      slotType: z.enum(['input', 'output', 'fuel']).optional().describe('Slot role (for slot/big_slot)'),
      ioMode: z.enum(['input', 'output', 'display']).optional().describe('I/O mode: input buffer, output buffer, or display only'),
    })).default([]).describe('Additional components to add after preset'),
  },
  async (args) => {
    // Auto-navigate to GUI tab
    wsSend({ type: 'set-tab', tab: 'gui' });
    if (args.preset !== 'none') {
      loadGuiPreset(args.preset);
    }
    for (const c of args.components) {
      addGuiComponent(c.type as GuiComponentType, c.x, c.y, {
        w: c.w, h: c.h, slotType: c.slotType as SlotType | undefined,
        ioMode: c.ioMode as IoMode | undefined,
      });
    }
    const state = getState();
    const comps = state.guiComponents.map((c, i) =>
      `  [${i}] ${c.type} at (${c.x},${c.y}) ${c.w}x${c.h}${c.slotType ? ` [${c.slotType}]` : ''} io:${c.ioMode}`
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
      `  [${i}] ${c.type} at (${c.x},${c.y}) ${c.w}x${c.h}${c.slotType ? ` [${c.slotType}]` : ''} io:${c.ioMode}`
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
  'Generate and write all files to the Retronism mod project directory. This writes Java source, JSON, and GUI script.',
  {},
  async () => {
    // Auto-save project before exporting
    let saveResult = '';
    try {
      const savedPath = saveProjectFile();
      saveResult = `Auto-saved project: ${savedPath}\n\n`;
    } catch (err: any) {
      saveResult = `Warning: auto-save failed: ${err.message}\n\n`;
    }

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

    return { content: [{ type: 'text', text: `${saveResult}Exported ${files.length} files:\n\n${results.join('\n')}` }] };
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
// Tool: set_port
// ---------------------------------------------------------------------------
server.tool(
  'set_port',
  'Set or clear port metadata on an existing block. Ports are IO config overlays — the block type stays the same. Set portType to configure as port, or omit to clear port config.',
  {
    x: z.number().int(),
    y: z.number().int(),
    z: z.number().int(),
    portType: z.enum(['energy', 'fluid', 'gas', 'item']).optional().describe('Port IO type. Omit to clear port config.'),
    mode: z.enum(['input', 'output', 'input_output']).optional().describe('Port direction (default: input_output)'),
  },
  async (args) => {
    const block = getBlockAt(args.x, args.y, args.z);
    if (!block) {
      return { content: [{ type: 'text', text: `No block at (${args.x}, ${args.y}, ${args.z}).` }] };
    }
    if (setPortType(args.x, args.y, args.z, args.portType as IOType | undefined, args.mode as PortMode | undefined)) {
      const action = args.portType ? `Set as ${args.portType} port (${args.mode || 'input_output'})` : 'Cleared port config';
      return { content: [{ type: 'text', text: `${action} on block at (${args.x}, ${args.y}, ${args.z}).\n\n${getStateSummary()}` }] };
    }
    return { content: [{ type: 'text', text: `Failed to set port at (${args.x}, ${args.y}, ${args.z}).` }] };
  },
);

// ---------------------------------------------------------------------------
// Tool: set_controller
// ---------------------------------------------------------------------------
server.tool(
  'set_controller',
  'Place a controller block at a position. Replaces whatever was there. Only one controller per structure. Pass coordinates + blockType to set, or clear=true to remove.',
  {
    x: z.number().int().optional(),
    y: z.number().int().optional(),
    z: z.number().int().optional(),
    blockType: z.string().optional().describe('Controller block ID (e.g. mega_crusher, mega_elec_ctrl). Defaults to first available controller.'),
    clear: z.boolean().optional().describe('Clear the current controller'),
  },
  async (args) => {
    if (args.clear) {
      setController(null);
      return { content: [{ type: 'text', text: `Controller cleared.\n\n${getStateSummary()}` }] };
    }
    if (args.x === undefined || args.y === undefined || args.z === undefined) {
      return { content: [{ type: 'text', text: 'Provide x, y, z coordinates or clear=true.' }] };
    }
    const key = `${args.x},${args.y},${args.z}`;
    setController(key, args.blockType);
    return { content: [{ type: 'text', text: `Set controller at (${args.x}, ${args.y}, ${args.z}).\n\n${getStateSummary()}` }] };
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
    defaultShellBlock: z.string().optional().describe('Block ID for shell filling (e.g. iron_block, stone)'),
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
      defaultShellBlock: args.defaultShellBlock,
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
        `  [${i}] ${c.type} at (${c.x},${c.y}) ${c.w}x${c.h}${c.slotType ? ` [${c.slotType}]` : ''} io:${c.ioMode}`
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
    ioMode: z.enum(['input', 'output', 'display']).optional().describe('I/O mode: input buffer, output buffer, or display only'),
  },
  async (args) => {
    if (updateGuiComponent(args.index, {
      x: args.x, y: args.y, w: args.w, h: args.h,
      slotType: args.slotType as any,
      ioMode: args.ioMode as any,
    })) {
      const state = getState();
      const comp = state.guiComponents[args.index];
      return { content: [{ type: 'text', text: `Updated component [${args.index}]: ${comp.type} at (${comp.x},${comp.y}) ${comp.w}x${comp.h}${comp.slotType ? ` [${comp.slotType}]` : ''} io:${comp.ioMode}` }] };
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
    category: z.enum(['controller', 'mod', 'vanilla', 'custom']).describe('Block category — determines behavior in structure validation and codegen'),
    label: z.string().describe('Display label (e.g., "Steel Casing")'),
    color: z.number().int().describe('Hex color for 3D preview (e.g., 0xAABBCC)'),
    char: z.string().max(1).optional().describe('Single char for serialization (auto-assigned if omitted)'),
    portType: z.enum(['energy', 'fluid', 'gas', 'item']).optional().describe('Port IO type (if this block acts as a default port type)'),
    tier: z.number().int().optional().describe('Optional tier number'),
    mcId: z.number().int().optional().describe('Minecraft block ID (required for vanilla blocks, used in checkStructure validation)'),
    terrainIndex: z.number().int().optional().describe('Tile index in terrain.png (16x16 grid) for texture in 3D editor'),
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
        mcId: args.mcId,
        terrainIndex: args.terrainIndex,
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
      if (b.mcId !== undefined) line += ` mcId=${b.mcId}`;
      if (b.terrainIndex !== undefined) line += ` terrain=${b.terrainIndex}`;
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

// ===========================================================================
// PROJECT PERSISTENCE
// ===========================================================================

const MULTIBLOCKS_DIR = path.resolve(import.meta.dirname, '..', '..', '..', 'multiblocks');

function ensureMultiblocksDir(): void {
  if (!fs.existsSync(MULTIBLOCKS_DIR)) fs.mkdirSync(MULTIBLOCKS_DIR, { recursive: true });
}

function saveProjectFile(name?: string): string {
  ensureMultiblocksDir();
  const data = serialize();
  const projectName = name || data.name;
  const filePath = path.join(MULTIBLOCKS_DIR, `${projectName}.json`);
  fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf-8');
  return filePath;
}

// Tool: save_project
server.tool(
  'save_project',
  'Save the current multiblock definition to multiblocks/{name}.json. This is the source of truth for the machine.',
  {
    name: z.string().optional().describe('Project name (default: current state name)'),
  },
  async (args) => {
    try {
      const filePath = saveProjectFile(args.name);
      return { content: [{ type: 'text', text: `Saved to ${filePath}\n\n${getStateSummary()}` }] };
    } catch (err: any) {
      return { content: [{ type: 'text', text: `Error saving: ${err.message}` }] };
    }
  },
);

// Tool: load_project
server.tool(
  'load_project',
  'Load a multiblock definition from multiblocks/{name}.json. Replaces current state.',
  {
    name: z.string().describe('Project name (without .json extension)'),
  },
  async (args) => {
    ensureMultiblocksDir();
    const filePath = path.join(MULTIBLOCKS_DIR, `${args.name}.json`);
    try {
      const raw = fs.readFileSync(filePath, 'utf-8');
      const data = JSON.parse(raw);
      deserialize(data);
      return { content: [{ type: 'text', text: `Loaded "${args.name}" from ${filePath}\n\n${getStateSummary()}` }] };
    } catch (err: any) {
      return { content: [{ type: 'text', text: `Error loading: ${err.message}` }] };
    }
  },
);

// Tool: list_projects
server.tool(
  'list_projects',
  'List all saved multiblock projects in the multiblocks/ directory.',
  {},
  async () => {
    ensureMultiblocksDir();
    try {
      const files = fs.readdirSync(MULTIBLOCKS_DIR).filter(f => f.endsWith('.json'));
      const names = files.map(f => f.replace('.json', ''));
      if (names.length === 0) {
        return { content: [{ type: 'text', text: 'No saved projects found in multiblocks/' }] };
      }
      return { content: [{ type: 'text', text: `Saved projects (${names.length}):\n${names.map(n => `  - ${n}`).join('\n')}` }] };
    } catch (err: any) {
      return { content: [{ type: 'text', text: `Error listing: ${err.message}` }] };
    }
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
      if (block.portType) {
        portDetails.push({ blockId: block.blockId, pos: key, mode: block.mode, portType: block.portType });
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
${state.guiComponents.map((c, i) => `  <component index="${i}" type="${c.type}" x="${c.x}" y="${c.y}" w="${c.w}" h="${c.h}"${c.slotType ? ` slotType="${c.slotType}"` : ''} ioMode="${c.ioMode}" />`).join('\n')}
</gui>

${modelInfo}

## Notes for Blockbench

### For multiblock structures:
- The Blockbench model represents the ENTIRE FORMED STRUCTURE, not just the controller block
- Use the \`<modelSize>\` dimensions above as the Blockbench canvas size (e.g., 48x64x48 pixels for a 3x3x4 structure)
- Design as one unified industrial machine — NOT a collection of separate casing blocks
- When the multiblock forms in-game, casing blocks become invisible and this model renders at the controller's position
- The controller stores its offset within the structure (\`structOffX/Y/Z\`) to align the model correctly
- The FORMED_PARTS array uses structure-space pixel coordinates (0 to modelSize width/height/depth)

### For single-block machines:
- Coordinates range 0-16 per axis (one block = 16 pixels)
- 8-15 elements for visual richness

### Render system (both types):
- Uses \`setBlockBounds\` + \`renderStandardBlock\` (axis-aligned boxes only, no rotations)
- \`setBlockBounds\` CAN go beyond 0.0-1.0 for multiblock formed models
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
    // Auto-save project before building
    let saveResult = '';
    try {
      const savedPath = saveProjectFile();
      saveResult = `Auto-saved project: ${savedPath}\n\n`;
    } catch (err: any) {
      saveResult = `Warning: auto-save failed: ${err.message}\n\n`;
    }

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

    return { content: [{ type: 'text', text: `${saveResult}Build complete — ${files.length} files exported:\n\n${results.join('\n')}${guiResult}` }] };
  },
);

// ---------------------------------------------------------------------------
// UI CONTROL TOOLS (WebSocket commands to Mod Maker app)
// ---------------------------------------------------------------------------

server.tool(
  'set_camera',
  'Set the 3D camera position in the Mod Maker viewer. Use preset angles or manual theta/phi/radius.',
  {
    preset: z.enum(['front', 'back', 'left', 'right', 'top', 'bottom', 'isometric']).optional()
      .describe('Named camera angle preset'),
    theta: z.number().optional().describe('Horizontal orbit angle in radians'),
    phi: z.number().optional().describe('Vertical orbit angle in radians (0=top, PI=bottom)'),
    radius: z.number().optional().describe('Distance from target'),
  },
  async ({ preset, theta, phi, radius }) => {
    const cam: Record<string, number> = {};
    if (preset) {
      switch (preset) {
        case 'front':      cam.theta = 0; cam.phi = Math.PI / 2; break;
        case 'back':       cam.theta = Math.PI; cam.phi = Math.PI / 2; break;
        case 'left':       cam.theta = -Math.PI / 2; cam.phi = Math.PI / 2; break;
        case 'right':      cam.theta = Math.PI / 2; cam.phi = Math.PI / 2; break;
        case 'top':        cam.theta = 0; cam.phi = 0.15; break;
        case 'bottom':     cam.theta = 0; cam.phi = Math.PI - 0.15; break;
        case 'isometric':  cam.theta = 0.6; cam.phi = 0.8; break;
      }
    }
    if (theta !== undefined) cam.theta = theta;
    if (phi !== undefined) cam.phi = phi;
    if (radius !== undefined) cam.radius = radius;
    wsSend({ type: 'camera', payload: cam });
    return { content: [{ type: 'text', text: `Camera set: ${JSON.stringify(cam)}` }] };
  },
);

server.tool(
  'set_tab',
  'Switch the active tab in the Mod Maker UI (structure editor or GUI builder).',
  {
    tab: z.enum(['structure', 'gui']).describe('Tab to activate'),
  },
  async ({ tab }) => {
    wsSend({ type: 'tab', payload: { tab } });
    return { content: [{ type: 'text', text: `Switched to ${tab} tab` }] };
  },
);

server.tool(
  'select_block',
  'Select (highlight) a block position in the 3D structure editor.',
  {
    x: z.number().describe('X coordinate'),
    y: z.number().describe('Y coordinate'),
    z: z.number().describe('Z coordinate'),
  },
  async ({ x, y, z: bz }) => {
    wsSend({ type: 'select_block', payload: { key: `${x},${y},${bz}` } });
    return { content: [{ type: 'text', text: `Selected block at ${x},${y},${bz}` }] };
  },
);

server.tool(
  'set_layer',
  'Set the layer filter in the 3D structure editor. Use -1 to show all layers.',
  {
    layer: z.number().describe('Layer Y index (-1 = show all)'),
  },
  async ({ layer }) => {
    wsSend({ type: 'set_layer', payload: { layer } });
    return { content: [{ type: 'text', text: `Layer filter set to ${layer === -1 ? 'all' : layer}` }] };
  },
);

server.tool(
  'highlight_blocks',
  'Temporarily highlight specific blocks in the 3D viewer (flash effect).',
  {
    positions: z.array(z.object({
      x: z.number(), y: z.number(), z: z.number(),
    })).describe('List of block positions to highlight'),
    duration_ms: z.number().optional().default(1500).describe('Highlight duration in milliseconds'),
  },
  async ({ positions, duration_ms }) => {
    const keys = positions.map(p => `${p.x},${p.y},${p.z}`);
    wsSend({ type: 'highlight', payload: { keys, duration: duration_ms } });
    return { content: [{ type: 'text', text: `Highlighting ${keys.length} blocks for ${duration_ms}ms` }] };
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
