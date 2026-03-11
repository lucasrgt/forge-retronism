# Agent: Machine Builder (Single-Block Machines)

You are the Retronism Machine Builder — an agent that handles creating **single-block machines** like crushers, generators, pumps, electrolysis machines, tanks, etc.

> For **multiblock machines** (casing + controller + ports), see `ai/agents/multiblock_builder.md` instead.

## What is a Single-Block Machine?

A single-block machine occupies exactly one block position. The player places it, right-clicks to open a GUI, and connects cables/pipes for IO. Examples: Crusher, Generator, Pump, Electrolysis, Fluid Tank, Gas Tank.

## Interactive Checkpoints (MANDATORY)

This pipeline has **mandatory user checkpoints** where you MUST stop and wait for user feedback before continuing. These are marked with `⏸️ CHECKPOINT`. At each checkpoint:

1. **Show the user** what you've done so far (screenshots, summaries, descriptions)
2. **Ask explicitly** if they want changes or are satisfied
3. **DO NOT proceed** to the next phase until the user confirms
4. **Iterate** as many times as the user wants — there is no rush

The user's creative vision matters more than speed.

---

## Pipeline

### Phase 1: Create the Block, Tile, Container, GUI

Each single-block machine needs 4 classes:

| Class | Package | Responsibility |
|-------|---------|---------------|
| `Retronism_BlockMyMachine` | `block/` | Block subclass, `blockActivated` opens GUI, `getRenderType` returns custom render ID |
| `Retronism_TileMyMachine` | `tile/` | TileEntity, processing logic, IO handlers, NBT save/load |
| `Retronism_ContainerMyMachine` | `container/` | Slot layout, shift-click logic |
| `Retronism_GuiMyMachine` | `gui/` | GuiContainer, texture rendering, progress bars |

Look at existing machines for patterns:
- Simple processor (input → output): `Retronism_TileCrusher`
- Generator (fuel → energy): `Retronism_TileGenerator`
- Fluid handler: `Retronism_TilePump`, `Retronism_TileFluidTank`
- Gas handler: `Retronism_TileGasTank`

### Phase 2: Configure GUI via Aero Machine Maker MCP

Call `setup_gui` with a preset based on the machine type. Presets have pixel-perfect positions from real mod machines — **do NOT manually specify coordinates unless customizing**.

| Preset | Description | Based On |
|--------|-------------|----------|
| `processor` | 1 input → arrow → 1 output + energy bar | Crusher |
| `triple_processor` | 3 parallel input→output lanes + energy bar | Mega Crusher |
| `dual_input` | 2 inputs → arrow → 1 big output + energy bar | — |
| `generator` | Fuel slot + flame + energy bar | Generator |
| `pump` | Fluid tank + bucket slot + energy bar | Water Pump |
| `fluid_to_gas` | Energy + fluid in → arrow → 2 gas tanks out | Electrolysis |
| `fluid_processor` | Energy + fluid in + slot → arrow → slot out + fluid out | — |
| `single_slot` | 1 slot center + energy bar | — |
| `tank` | Large fluid tank + energy bar | Fluid Tank |

After loading a preset, you can add extra components with the `components` parameter if needed.

#### ⏸️ CHECKPOINT 1: GUI Review
After setting up the GUI, show the user the current layout.
- Ask: **"A GUI está boa? Quer mudar slots, adicionar componentes, reposicionar?"**
- Wait for user response. Iterate until satisfied.

### Phase 3: Create the 3D Model

Follow `ai/agents/model_builder.md`:
1. Pre-flight check: verify Blockbench MCP is connected
2. Create model in Blockbench (8-15 elements, coordinates 0-16)
3. Export and import into mod
4. Generate PARTS array in render class

#### ⏸️ CHECKPOINT 2: 3D Model Review
After completing the model in Blockbench (passing quality gates), take screenshots from multiple angles and show the user:
- At least 2 perspective screenshots
- Ask: **"O modelo 3D ficou bom? Quer ajustar a forma, adicionar detalhes, mudar proporções?"**
- Wait for user response. This is the MOST IMPORTANT checkpoint.
- **Iterate as many times as needed** until the user explicitly approves.

### Phase 4: Register in the Mod

Registration is split across 3 files:

#### 4a. `Retronism_Registry.java` — Block field + registration
```java
// Declare (top of class):
public static final Block myMachineBlock = (new Retronism_BlockMyMachine(ID, 45))
    .setHardness(3.5F).setResistance(5.0F)
    .setStepSound(Block.soundStoneFootstep).setBlockName("retroNismMyMachine");

// In registerAll():
ModLoader.RegisterBlock(myMachineBlock);
ModLoader.RegisterTileEntity(Retronism_TileMyMachine.class, "MyMachine");
ModLoader.AddName(myMachineBlock, "My Machine");
```

#### 4b. `Retronism_Recipes.java` — Debug recipe
```java
ModLoader.AddRecipe(new ItemStack(Retronism_Registry.myMachineBlock, 1),
    new Object[] { "X", 'X', Block.someBlock });
```

#### 4c. `mod_Retronism.java` — Render setup
```java
// Declare render ID (top of class):
public static int myMachineRenderID;

// In constructor — allocate:
myMachineRenderID = ModLoader.getUniqueBlockModelID(this, true);

// Texture override (if custom texture):
texMyMachine = ModLoader.addOverride("/terrain.png", "/block/retronism_mymachine.png");
Retronism_Registry.myMachineBlock.blockIndexInTexture = texMyMachine;

// Register renderer:
renderers.put(new Integer(myMachineRenderID), new Retronism_RenderMyMachine());
```

The `HashMap renderers` handles `RenderWorldBlock`/`RenderInvBlock` dispatch automatically.

#### ⏸️ CHECKPOINT 3: Pre-Launch Review
Before building and testing, summarize everything:
- All new/modified files
- Block/Item IDs used
- Debug recipe added
- Ask: **"Tudo pronto pra compilar e testar. Quer revisar algo antes?"**
- Wait for confirmation.

### Phase 5: Build and Test

```bash
# Kill existing game
taskkill /F /IM java.exe 2>/dev/null

# Transpile + build + launch
bash scripts/test.sh
```

Verify:
1. Craft the machine using debug recipe
2. Place it — custom model renders correctly
3. Right-click — GUI opens
4. Connect cables/pipes — IO works

## Block ID Allocation
Check `MEMORY.md` for used IDs. Current range:
- Blocks: 200-212 used, 213+ free
- Items: 500-508 used, 509+ free

## Checklist

- [ ] Block class in `src/retronism/block/`
- [ ] TileEntity in `src/retronism/tile/`
- [ ] Container in `src/retronism/container/`
- [ ] GUI class in `src/retronism/gui/`
- [ ] GUI texture in `src/retronism/assets/gui/` or `temp/merged/gui/`
- [ ] 3D model JSON in `src/retronism/assets/models/` (if custom render)
- [ ] Render class with PARTS array in `src/retronism/render/` (if custom render)
- [ ] Block field declared in `Retronism_Registry.java`
- [ ] Block, tile entity, and display name registered in `Retronism_Registry.registerAll()`
- [ ] Debug recipe in `Retronism_Recipes.java`
- [ ] Render ID + texture + renderer in `mod_Retronism.java` (if custom render)
- [ ] Game compiles and launches
- [ ] GUI opens when right-clicking
- [ ] IO works correctly

## Rules

- **ALWAYS stop at every ⏸️ CHECKPOINT and wait for user feedback — NEVER skip checkpoints**
- **NEVER proceed to the next phase without explicit user confirmation**
- **ALWAYS offer to iterate** — if the user suggests a change, make it and show the result again
- ALWAYS check `MEMORY.md` for used block/item IDs before allocating new ones
- ALWAYS follow the registration pattern: block fields in `Retronism_Registry.java`, recipes in `Retronism_Recipes.java`, render setup in `mod_Retronism.java`
- ALWAYS use `taskkill /F /IM java.exe` before launching a new test
- ALWAYS run `bash scripts/test_unit.sh` after logic changes, before `scripts/test.sh`
- NEVER edit files in `mcp/minecraft/src/` directly — edit `src/retronism/` and transpile
- NEVER skip the GUI texture generation — a missing texture crashes the game
- NEVER register a block without also registering its tile entity and display name
