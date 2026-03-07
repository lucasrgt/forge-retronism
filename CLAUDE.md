# Retronism - Minecraft Beta 1.7.3 Tech Mod

## Workflow
- Edit mod source ONLY in `src/retronism/` (organized packages)
- NEVER edit `mcp/minecraft/src/net/minecraft/src/Retronism_*.java` directly — those are transpiled output
- `bash scripts/test.sh` auto-transpiles → builds → injects → launches
- `bash scripts/test_unit.sh` auto-transpiles → recompiles → runs JUnit tests
- The "official" build is `mcp/build/minecraft.zip` (clean, no TMI/SPC)

## Build Commands
- Test (in-game): `bash scripts/test.sh` (transpile + build + launch)
- Unit tests: `bash scripts/test_unit.sh` (transpile + recompile + JUnit)
- Transpile only: `bash scripts/transpile.sh` (src/retronism/ → mcp/minecraft/src/)
- Build only: `cd mcp && echo "build" | java -jar RetroMCP-Java-CLI.jar`
- Update MD5 (after editing vanilla files): `cd mcp && echo "updatemd5" | java -jar RetroMCP-Java-CLI.jar`

## Transpile System
- Source of truth: `src/retronism/` with organized Java packages
- `scripts/transpile.sh` flattens to `mcp/minecraft/src/net/minecraft/src/` for RetroMCP
- Rewrites `package retronism.xxx;` → `package net.minecraft.src;`
- Removes `import retronism.*` and `import net.minecraft.src.*` (redundant after flatten)
- Preserves external imports (org.lwjgl, java.util, etc.)
- Also copies `src/retronism/assets/` → `temp/merged/` (textures for jar injection)

## Unit Tests
- Test source: `testing/src/net/minecraft/src/*Test.java` (never goes into build)
- Dependencies: `testing/libs/junit-4.13.2.jar`, `testing/libs/hamcrest-core-1.3.jar`
- Compiles against `mcp/minecraft/bin/` (deobfuscated classes from RetroMCP)
- After any logic changes to handlers/APIs, run `bash scripts/test_unit.sh` before `scripts/test.sh`

## Project Structure
- **Mod source (edit here):** `src/retronism/` with subpackages:
  - `api/` — interfaces (IEnergyReceiver, IFluidHandler, IGasHandler) and types (FluidType, GasType)
  - `block/` — Block subclasses
  - `tile/` — TileEntity subclasses
  - `gui/` — GuiContainer subclasses
  - `container/` — Container subclasses
  - `item/` — Item subclasses
  - `recipe/` — Recipe registries
  - `slot/` — Custom Slot subclasses
  - `assets/gui/` — GUI textures (*.png), copied to temp/merged/ by transpiler
  - `mod_Retronism.java` — main mod class (package root)
- **Transpiled output (don't edit):** `mcp/minecraft/src/net/minecraft/src/`
- **MCP ecosystem:** `mcp/` (minecraft, minecraft_server, libraries, jars, deps, conf, build, RetroMCP-Java-CLI.jar, options.cfg)
- **Scripts:** `scripts/` (test.sh, test_unit.sh, transpile.sh)
- **Tests:** `tests/` (data/, libs/, out/, src/)
- Mod classes use `Retronism_` prefix (e.g., `Retronism_BlockCable`, `Retronism_ItemTest`)
- Block IDs start at 200, Item IDs start at 500

## Running the game
- Always kill existing java proccesses and then start the game again after updating the code 

## Agents (on-demand context loading)
- When generating GUI textures, or the user mentions GUI/textura, READ `ai/agents/gui_builder.md` FIRST — it has the full API, palette, coordinates and workflow
- When creating 3D models for machines, READ `ai/agents/model_builder.md` FIRST — Blockbench constraints, design philosophy, UV rules, render integration
- When creating a new machine end-to-end (structure → model → code → test), READ `ai/agents/machine_builder.md` FIRST — full pipeline orchestrator
- Always use `tools/gui_builder.py` — never hand-draw pixels

## Language
- User communicates in Portuguese (BR). Respond in Portuguese.
