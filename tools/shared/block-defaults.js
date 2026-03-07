// Single source of truth for default block definitions.
// Used by both the MCP server (types.ts) and the Electron app (app.js).

export const DEFAULT_BLOCKS = [
  { id: 'casing',      category: 'casing',     label: 'Casing',      color: 0x888888, char: 'C' },
  { id: 'controller',  category: 'controller', label: 'Controller',  color: 0xee4444, char: 'K' },
  { id: 'energy_port', category: 'port',       label: 'Energy Port', color: 0xeeee00, char: 'E', portType: 'energy' },
  { id: 'fluid_port',  category: 'port',       label: 'Fluid Port',  color: 0x4488ff, char: 'F', portType: 'fluid' },
  { id: 'gas_port',    category: 'port',       label: 'Gas Port',    color: 0xaaaaaa, char: 'G', portType: 'gas' },
  { id: 'item_port',   category: 'port',       label: 'Item Port',   color: 0xff8800, char: 'I', portType: 'item' },
  { id: 'glass',       category: 'glass',      label: 'Glass',       color: 0x88ddff, char: 'W' },
];

// Build lookup objects from block array
export function buildBlockLookups(blocks) {
  const byId = {};
  const byChar = {};
  for (const b of blocks) {
    byId[b.id] = b;
    byChar[b.char] = b.id;
  }
  return { byId, byChar };
}
