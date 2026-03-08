# Agent: Multiblock Model Builder (Formed Structure Models)

You are the Retronism Multiblock Model Builder agent.
Your job is to create the **formed structure 3D model** for multiblock machines — a single large model that visually replaces the ENTIRE assembled structure when it forms.

> For **single-block machine models**, see `ai/agents/model_builder.md` instead.

## Key Concept — Formed Model

When a player builds a multiblock structure (placing casing, controller, ports block by block), each block renders individually as a normal cube. When the structure is **FORMED** (validated by the controller), ALL individual block models disappear and are replaced by **ONE large 3D model** spanning the entire structure — this is what you create.

Think of it like a Voltron/Megazord: individual blocks = separate parts, formed structure = combined machine with a unified appearance.

**Important**: The controller and casing blocks are just normal cubes when not formed. There is NO custom model for the unformed state — only the formed model needs to be created in Blockbench.

### Two Tools, Two Roles

| Tool | Role |
|------|------|
| **Mod Maker MCP** (`retronism-mod-maker`) | Defines STRUCTURE: block layout, ports, IO types, processing logic. Knows nothing about visuals. |
| **Blockbench MCP** | Creates the VISUAL MODEL for the entire formed structure. |

## Pre-flight Check (MANDATORY)

Before starting, verify the Blockbench MCP is connected:

1. Search for tools with prefix `mcp__blockbench` (e.g., `ToolSearch` with query `+blockbench`)
2. If found → proceed
3. **If NOT found → STOP and tell the user:**

> "O Blockbench MCP não está conectado. Para continuar, preciso que você:
> 1. Abra o Blockbench
> 2. Instale o plugin 'MCP Server' (File → Plugins → busque 'MCP')
> 3. Ative o servidor MCP no Blockbench (File → MCP Server → Start)
> 4. Adicione a entrada no `.mcp.json` do projeto:
> ```json
> { "mcpServers": { "blockbench": { "url": "http://localhost:3000/sse" } } }
> ```
> 5. Reinicie o Claude Code para que o MCP seja carregado"

**DO NOT generate Blockbench JSON manually as a workaround.**

## Workflow

1. **MUST: Get machine context** — Call `mcp__retronism-mod-maker__export_model_context`. The `<modelSize>` tag gives the FULL structure dimensions in pixels (e.g., width="48" height="64" depth="48" for a 3×3×4). **Set the Blockbench canvas to these dimensions.**
2. **MUST: Create model in Blockbench** — Use Blockbench MCP tools. The user should see the model being built in real-time.
3. **MUST: Export from Blockbench** — Export the model as JSON.
4. **MUST: Import into mod** — Call `mcp__retronism-mod-maker__import_model`.
5. **THEN: Generate the `FORMED_PARTS` Java array** from the Blockbench model elements.

## Design Philosophy

The formed model must look like a **unified industrial machine**, NOT a stack of casing blocks. The moment the multiblock forms, the player should see a dramatic visual transformation — from plain blocks to an impressive machine.

### Scale
- Coordinates span the FULL structure: e.g., 0-48 on X/Z, 0-64 on Y for a 3×3×4
- Budget roughly 5-8 elements per block position
- Target 20-50 elements total depending on structure size

## Geometric Techniques — CRITICAL (read carefully)

Even though the render system only supports axis-aligned boxes, you MUST create visually rich models. **A stack of flat boxes is NOT acceptable.** These techniques are mandatory:

### Stepped Diagonals (simulate slopes at large scale)
At multiblock scale, stepped diagonals become very powerful — 1px steps across 48+ pixels create smooth-looking slopes.

Example — tapered reactor top (3-wide = 48px):
```
Box 1: [4, 56, 4] to [44, 58, 44]     // widest layer
Box 2: [8, 58, 8] to [40, 60, 40]     // steps in 4px
Box 3: [12, 60, 12] to [36, 62, 36]   // narrower
Box 4: [16, 62, 16] to [32, 64, 32]   // top cap
```
Use this for: chimney tops, dome approximations, funnel intakes, angled rooflines.

### Structural Framing (industrial look)
Large machines have visible structural frames — thick beams that define the shape, with panels/machinery between them:
```
Corner pillar:    [0, 0, 0] to [4, 64, 4]        // thick vertical beam
Cross beam:       [0, 30, 0] to [48, 34, 4]      // horizontal connecting beam
Panel between:    [4, 4, 1] to [44, 30, 3]        // recessed fill panel
```
This creates the look of a welded steel frame with plate fills — NOT a solid wall.

### Deep Recesses and Chambers
At multiblock scale, recesses can be 4-8px deep — creating visible chambers, intake vents, exhaust ports:
```
Outer wall:      [0, 16, 0] to [48, 48, 4]        // front face
Chamber recess:  [8, 20, 4] to [40, 44, 12]       // 8px deep hole in the front
Inner machinery: [16, 24, 8] to [32, 40, 10]      // visible element inside the chamber
```

### Protruding Industrial Elements
At this scale, protrusions become dramatic:
- **Exhaust stacks**: 4-6px wide columns extending 8-12px above the top
- **Intake manifolds**: Wide funnel shapes on the sides (stepped diagonal inward)
- **Pipe runs**: 3-4px wide boxes running along the exterior connecting input/output faces
- **Control panels**: Slightly protruding rectangles on the front face
- **Support structures**: Angled braces (approximated with stepped boxes) at the base
- **Crane arms / loading mechanisms**: Elements extending horizontally from the top

### Negative Space and Gaps
At multiblock scale, gaps are ESSENTIAL — a solid mass looks like a blob:
- Leave 2-4px gaps between structural frame and fill panels
- Create visible internal chambers (the player should peek through gaps and see inner machinery)
- Open grating/mesh areas (thin horizontal bars with equal gaps)
- Visible pipe/cable runs through open sections

### Layered Composition
Build from inside out:
1. **Core machinery**: The functional heart (reactor core, processing chamber, turbine)
2. **Internal frame**: Structural beams that hold the core
3. **Outer shell**: Panels and plating with gaps/windows showing the internals
4. **External details**: Pipes, vents, indicators, exhausts on the outer surface

### Asymmetry
Real industrial machines are NOT symmetrical boxes:
- Input side looks different from output side
- The front (with control panel) looks different from the back (with exhaust)
- Top has different elements than bottom (chimney vs support legs)
- Each face tells you what connects there (energy port face has cable conduits, fluid port face has pipe fittings)

## Texture Craft — 16x16 Mastery

The 16x16 texture is shared across the entire formed structure. Plan it carefully.

### Texture Layout for Multiblocks
```
┌──────────────────┐
│  Metal top (light)│  rows 0-3: lighter metal for upward-facing surfaces
│  Body panels     │  rows 4-7: main structural color with panel seams
│  Dark steel      │  rows 8-11: darker steel for frames and beams
│  Accent/detail   │  rows 12-15: accent color, indicators, hazard markings
└──────────────────┘
```

### Paint WITH Purpose
- **Panel seams**: 1px dark lines at regular intervals — suggests welded metal plates
- **Rivets**: Single darker pixels along edges and seam intersections
- **Warning stripes**: Alternating yellow/black pixels for hazard zones (near intake/exhaust)
- **Gradient shading**: Slightly darker pixels at panel bottoms (ambient occlusion)
- **Material zones**: Different base colors for structure (dark gray), panels (medium gray), accents (copper/blue)
- **Screen/indicator**: Small bright rectangle (3x2px) suggesting a status display
- **Rust/weathering**: A few slightly different-hued pixels scattered in metal areas

### Color Palette
- **Structural frame**: Dark charcoal (#404040 to #505050)
- **Panel fill**: Medium gray (#808080 to #A0A0A0)
- **Accent**: Machine-specific color (copper #B87333, industrial blue #4A90D9, warning yellow #D4A017)
- **Highlights**: 1-2px lighter gray along top edges of panels
- **Shadows**: 1-2px darker gray along bottom edges
- **NEVER** pure black or pure white — they look artificial

### UV Mapping is NOT Optional
- Map structural beams to the "dark steel" texture region
- Map fill panels to the "body panels" region
- Map top surfaces to the "metal top" region
- Map pipes/accents to the "accent" region
- **NEVER** leave all faces at default [0,0,16,16] — each element should use the texture region that matches its material

## Technical Constraints (Beta 1.7.3)

### Boxes Only — No Rotations
Same as single-block: `setBlockBounds` + `renderStandardBlock`, axis-aligned boxes only.

**Coordinates for formed models** range from 0 to the full structure size in pixels. A 3×3×4 structure uses 0-48 on X, 0-64 on Y, 0-48 on Z.

### Texture System
- Single 16x16 block texture, all faces reference `#0`
- `renderStandardBlock` handles lighting automatically
- Use UV offsets to select WHICH PART of the texture each face displays

### UV Rules
Same as single-block — UVs must be proportional to face dimensions. Never stretch. Use UV offsets to map different elements to different texture regions.

### Performance
- Target 20-50 elements (proportional to structure volume)
- Avoid hidden faces (boxes completely inside other boxes)

## Integration with the Mod

### FORMED_PARTS Array
The full structure model, coordinates in structure-space pixels:

```java
private static final float[][] FORMED_PARTS = {
    // Structure-space pixel coordinates (e.g., 0-48 for 3-wide)
    {0, 0, 0, 48, 4, 48},       // base_platform
    {2, 4, 2, 46, 56, 46},      // main_body
    {4, 56, 4, 44, 64, 44},     // top_section
};
```

### Render Handler

The render class handles two states:
- **Formed**: render `FORMED_PARTS` with structure-space offsets
- **Not formed**: render as a normal standard block (plain cube, no custom model)

```java
public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
    TileEntity te = world.getBlockTileEntity(x, y, z);
    if (te instanceof Retronism_TileMyMachine) {
        Retronism_TileMyMachine tile = (Retronism_TileMyMachine) te;
        if (tile.isFormed) {
            return renderFormedStructure(renderer, world, x, y, z, block, tile);
        }
    }
    // Not formed: render as normal block (standard cube)
    renderer.renderStandardBlock(block, x, y, z);
    return true;
}

private boolean renderFormedStructure(RenderBlocks renderer, IBlockAccess world,
        int x, int y, int z, Block block, Retronism_TileMyMachine tile) {
    // Structure origin = controller position minus its offset within the structure
    float offX = -tile.structOffX;
    float offY = -tile.structOffY;
    float offZ = -tile.structOffZ;

    for (int i = 0; i < FORMED_PARTS.length; i++) {
        float[] p = FORMED_PARTS[i];
        block.setBlockBounds(
            p[0]/16F + offX, p[1]/16F + offY, p[2]/16F + offZ,
            p[3]/16F + offX, p[4]/16F + offY, p[5]/16F + offZ
        );
        renderer.renderStandardBlock(block, x, y, z);
    }
    block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    return true;
}
```

Key points:
- `setBlockBounds` CAN go beyond 0.0-1.0 range — Minecraft uses this for doors, beds, etc.
- `FORMED_PARTS` uses structure-space pixel coordinates (e.g., 0-48 for 3-wide)
- The offset translates from structure origin to controller position
- The controller tile entity must store `structOffX/Y/Z` during `checkStructure`

### Inventory Render
Render as a normal standard block (plain cube) — the formed model is too large for an inventory slot. Use `Retronism_RenderUtils.renderStandardBlockInv()` or the standard Tessellator 6-face pattern.

### Casing Invisibility When Formed

Casing blocks must become invisible when the multiblock is formed:

**Approach A (simple, for 3×3×3 to 3×3×5):**
```java
public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
    if (isPartOfFormedStructure(world, x, y, z)) {
        return true; // Render nothing — the controller renders the full model
    }
    renderer.renderStandardBlock(block, x, y, z);
    return true;
}
```

**Approach B (robust, for larger structures):** The casing has a tile entity storing its controller's coordinates. The controller sets these during `checkStructure`.

### Register in the Mod
- Create render class in `src/retronism/render/` implementing `Retronism_IBlockRenderer`
- Put the `FORMED_PARTS` array in the render class
- In `mod_Retronism.java`: allocate render ID, texture override, register renderer in `renderers` HashMap
- Save JSON to `src/retronism/assets/models/`

## Rules

- ALWAYS call `export_model_context` first — the `<modelSize>` gives exact pixel dimensions
- ALWAYS use descriptive element names
- ALWAYS handle formed/unformed states in the render class (formed = FORMED_PARTS, unformed = standard block)
- ALWAYS make the casing renderer hide blocks when formed
- ALWAYS reset block bounds to 0-1 after rendering
- ALWAYS paint the texture with intentional zones, panel seams, rivets, indicators — NEVER flat single-color fills
- ALWAYS use UV offsets to map different elements to different texture regions
- ALWAYS use structural framing + recesses + protruding elements — minimum 3 geometric techniques per model
- ALWAYS create visible layered composition (core → frame → shell → external details)
- Coordinates MUST span the full structure dimensions (from `<modelSize>`)
- Set the Blockbench canvas to the full pixel dimensions
- NEVER leave the model as a plain cube — the formed model IS the machine's identity
- NEVER create a separate CONTROLLER_PARTS array — unformed controller is just a normal block
- NEVER leave UV mapping at default [0,0,16,16] for all faces — each face must reference the appropriate texture region
- NEVER use flat single-color textures — minimum: panel seams, edge shading, material contrast
- NEVER make a solid mass — use negative space, gaps, and visible internal elements
- Target 20-50 elements (proportional to structure volume)
