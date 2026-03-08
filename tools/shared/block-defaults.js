// Single source of truth for default block definitions.
// Used by both the MCP server (types.ts) and the Electron app (app.js).

export const DEFAULT_BLOCKS = [
  // ── Controller blocks (one per multiblock machine) ────────────────────
  { id: 'mega_crusher',    category: 'controller', label: 'Mega Crusher Controller',      color: 0x6a6a6a, char: 'y', mcId: 212, terrainIndex: 45 },
  { id: 'mega_elec_ctrl',  category: 'controller', label: 'Mega Electrolysis Controller', color: 0x5599aa, char: 'z', mcId: 213, terrainIndex: 45 },

  // ── Mod blocks (existing RetroNism blocks) ────────────────────────────
  { id: 'test_block',      category: 'mod', label: 'Test Block',       color: 0x8888cc, char: 'E', mcId: 200, terrainIndex: 1 },
  { id: 'cable',           category: 'mod', label: 'Cable',            color: 0xc8c800, char: 'F', mcId: 201, terrainIndex: 22 },
  { id: 'crusher',         category: 'mod', label: 'Crusher',          color: 0x8a8a8a, char: 'G', mcId: 202, terrainIndex: 45 },
  { id: 'generator',       category: 'mod', label: 'Generator',        color: 0x8a6e4a, char: 'I', mcId: 203, terrainIndex: 45 },
  { id: 'pump',            category: 'mod', label: 'Water Pump',       color: 0x4488cc, char: 'i', mcId: 204, terrainIndex: 45 },
  { id: 'fluid_pipe',      category: 'mod', label: 'Fluid Pipe',       color: 0x3366cc, char: 'k', mcId: 205, terrainIndex: 23 },
  { id: 'electrolysis',    category: 'mod', label: 'Electrolysis',     color: 0x66aacc, char: 'l', mcId: 206, terrainIndex: 45 },
  { id: 'gas_pipe',        category: 'mod', label: 'Gas Pipe',         color: 0xaaaaaa, char: 'q', mcId: 207, terrainIndex: 54 },
  { id: 'fluid_tank',      category: 'mod', label: 'Fluid Tank',       color: 0x2255aa, char: 'r', mcId: 208, terrainIndex: 45 },
  { id: 'gas_tank',        category: 'mod', label: 'Gas Tank',         color: 0x999999, char: 's', mcId: 209, terrainIndex: 45 },
  { id: 'mega_pipe',       category: 'mod', label: 'Mega Pipe',        color: 0xcc8844, char: 't', mcId: 210, terrainIndex: 22 },
  { id: 'item_pipe',       category: 'mod', label: 'Item Pipe',        color: 0xee8833, char: 'v', mcId: 211, terrainIndex: 22 },
  { id: 'mega_elec_case',  category: 'mod', label: 'Mega Electrolysis Casing', color: 0x778888, char: 'Z', mcId: 214, terrainIndex: 45 },
  { id: 'machine_port',    category: 'port', label: 'Machine Port',             color: 0xc8c800, char: '1', mcId: 216, terrainIndex: 45 },

  // ── Vanilla blocks (Beta 1.7.3) ────────────────────────────────────────
  { id: 'glass',              category: 'vanilla', label: 'Glass',              color: 0x88ddff, char: 'W', mcId: 20, terrainIndex: 49 },
  // mcId = Block ID in Minecraft, terrainIndex = tile index in terrain.png
  { id: 'stone',              category: 'vanilla', label: 'Stone',              color: 0x7d7d7d, char: 'S',  mcId: 1,  terrainIndex: 1 },
  { id: 'dirt',               category: 'vanilla', label: 'Dirt',               color: 0x866043, char: 'D',  mcId: 3,  terrainIndex: 2 },
  { id: 'cobblestone',        category: 'vanilla', label: 'Cobblestone',        color: 0x7a7a7a, char: 'b',  mcId: 4,  terrainIndex: 16 },
  { id: 'planks',             category: 'vanilla', label: 'Planks',             color: 0xbc9862, char: 'P',  mcId: 5,  terrainIndex: 4 },
  { id: 'bedrock',            category: 'vanilla', label: 'Bedrock',            color: 0x555555, char: 'B',  mcId: 7,  terrainIndex: 17 },
  { id: 'sand',               category: 'vanilla', label: 'Sand',               color: 0xdbd3a0, char: 'a',  mcId: 12, terrainIndex: 18 },
  { id: 'gravel',             category: 'vanilla', label: 'Gravel',             color: 0x857b7b, char: 'g',  mcId: 13, terrainIndex: 19 },
  { id: 'ore_gold',           category: 'vanilla', label: 'Gold Ore',           color: 0x8f8258, char: '4',  mcId: 14, terrainIndex: 32 },
  { id: 'ore_iron',           category: 'vanilla', label: 'Iron Ore',           color: 0x87796d, char: '5',  mcId: 15, terrainIndex: 33 },
  { id: 'ore_coal',           category: 'vanilla', label: 'Coal Ore',           color: 0x737373, char: '6',  mcId: 16, terrainIndex: 34 },
  { id: 'log',                category: 'vanilla', label: 'Log',                color: 0x6b5130, char: 'L',  mcId: 17, terrainIndex: 20 },
  { id: 'sponge',             category: 'vanilla', label: 'Sponge',             color: 0xc3c33a, char: 'Q',  mcId: 19, terrainIndex: 48 },
  { id: 'lapis_ore',          category: 'vanilla', label: 'Lapis Ore',          color: 0x6b7fa0, char: '7',  mcId: 21, terrainIndex: 160 },
  { id: 'lapis_block',        category: 'vanilla', label: 'Lapis Block',        color: 0x1e3a82, char: 'c',  mcId: 22, terrainIndex: 144 },
  { id: 'dispenser',          category: 'vanilla', label: 'Dispenser',          color: 0x8a8a8a, char: 'd',  mcId: 23, terrainIndex: 46 },
  { id: 'sandstone',          category: 'vanilla', label: 'Sandstone',          color: 0xd8cb8c, char: 'e',  mcId: 24, terrainIndex: 192 },
  { id: 'note_block',         category: 'vanilla', label: 'Note Block',         color: 0x6b4632, char: 'f',  mcId: 25, terrainIndex: 74 },
  { id: 'wool',               category: 'vanilla', label: 'Wool',               color: 0xe8e8e8, char: 'w',  mcId: 35, terrainIndex: 64 },
  { id: 'gold_block',         category: 'vanilla', label: 'Gold Block',         color: 0xf5d900, char: 'H',  mcId: 41, terrainIndex: 23 },
  { id: 'iron_block',         category: 'vanilla', label: 'Iron Block',         color: 0xc8c8c8, char: 'R',  mcId: 42, terrainIndex: 22 },
  { id: 'brick',              category: 'vanilla', label: 'Brick',              color: 0x9b634a, char: 'J',  mcId: 45, terrainIndex: 7 },
  { id: 'tnt',                category: 'vanilla', label: 'TNT',                color: 0xdb3b24, char: 'T',  mcId: 46, terrainIndex: 8 },
  { id: 'bookshelf',          category: 'vanilla', label: 'Bookshelf',          color: 0x755c3a, char: 'h',  mcId: 47, terrainIndex: 35 },
  { id: 'mossy_cobblestone',  category: 'vanilla', label: 'Mossy Cobblestone',  color: 0x677967, char: 'M',  mcId: 48, terrainIndex: 36 },
  { id: 'obsidian',           category: 'vanilla', label: 'Obsidian',           color: 0x1b1029, char: 'O',  mcId: 49, terrainIndex: 37 },
  { id: 'diamond_ore',        category: 'vanilla', label: 'Diamond Ore',        color: 0x818f8f, char: '8',  mcId: 56, terrainIndex: 50 },
  { id: 'diamond_block',      category: 'vanilla', label: 'Diamond Block',      color: 0x6ee8e4, char: 'A',  mcId: 57, terrainIndex: 24 },
  { id: 'crafting_table',     category: 'vanilla', label: 'Crafting Table',     color: 0xb08b54, char: 'j',  mcId: 58, terrainIndex: 59 },
  { id: 'furnace',            category: 'vanilla', label: 'Furnace',            color: 0x8a8a8a, char: 'V',  mcId: 61, terrainIndex: 45 },
  { id: 'redstone_ore',       category: 'vanilla', label: 'Redstone Ore',       color: 0x8a6565, char: '9',  mcId: 73, terrainIndex: 51 },
  { id: 'ice',                category: 'vanilla', label: 'Ice',                color: 0x7dadff, char: 'X',  mcId: 79, terrainIndex: 67 },
  { id: 'snow_block',         category: 'vanilla', label: 'Snow Block',         color: 0xf0fafa, char: 'N',  mcId: 80, terrainIndex: 66 },
  { id: 'clay',               category: 'vanilla', label: 'Clay',               color: 0xa4a8b8, char: 'Y',  mcId: 82, terrainIndex: 72 },
  { id: 'jukebox',            category: 'vanilla', label: 'Jukebox',            color: 0x6b4632, char: 'U',  mcId: 84, terrainIndex: 74 },
  { id: 'netherrack',         category: 'vanilla', label: 'Netherrack',         color: 0x6f3535, char: 'n',  mcId: 87, terrainIndex: 103 },
  { id: 'soul_sand',          category: 'vanilla', label: 'Soul Sand',          color: 0x5b4538, char: 'u',  mcId: 88, terrainIndex: 104 },
  { id: 'glowstone',          category: 'vanilla', label: 'Glowstone',          color: 0xd9c477, char: 'x',  mcId: 89, terrainIndex: 105 },
  { id: 'pumpkin',            category: 'vanilla', label: 'Pumpkin',            color: 0xc67a1a, char: 'p',  mcId: 86, terrainIndex: 102 },
  { id: 'jack_o_lantern',     category: 'vanilla', label: "Jack o'Lantern",     color: 0xe8a030, char: 'o',  mcId: 91, terrainIndex: 102 },
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
