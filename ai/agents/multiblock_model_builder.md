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

## Pre-flight Check (MANDATORY - RUN FIRST)

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

---

## Blockbench MCP Tool Quick Reference

| Tool | Purpose | Key Params |
|------|---------|------------|
| `create_project` | New project | `name`, `format` (default "bedrock_block") |
| `add_group` | Create element group | `name`, `origin: [x,y,z]`, `rotation: [0,0,0]` |
| `place_cube` | Place one or more cubes | `elements: [{name, from, to}]`, `group`, `texture`, `faces: true` |
| `modify_cube` | Change cube position/size/UV | `id` (name), `from`, `to`, `uv_offset: [x,y]` |
| `duplicate_element` | Copy a cube with offset | `id` (name), `newName`, `offset: [x,y,z]` |
| `create_texture` | Create/fill texture | `name`, `width`, `height`, `fill_color` (hex or RGBA), `layer_name` |
| `paint_with_brush` | Paint pixels on texture | `coordinates: [{x,y}]`, `brush_settings: {color, size, opacity}`, `connect_strokes`, `texture_id` |
| `draw_shape_tool` | Draw rectangles/ellipses | `shape`, `start: {x,y}`, `end: {x,y}`, `color`, `texture_id` |
| `paint_fill_tool` | Bucket fill | `x`, `y`, `color`, `fill_mode`, `texture_id` |
| `gradient_tool` | Apply gradient | `start: {x,y}`, `end: {x,y}`, `start_color`, `end_color`, `blend_mode`, `texture_id` |
| `paint_settings` | Configure paint options | `pixel_perfect`, `mirror_painting: {enabled, axis}` |
| `set_camera_angle` | Position camera | `position: [x,y,z]`, `projection`, `target: [x,y,z]` |
| `capture_screenshot` | Take viewport screenshot | (none) |
| `capture_app_screenshot` | Full app screenshot | (none) |
| `list_outline` | Verify element hierarchy | (none) |
| `list_textures` | Verify textures exist | (none) |
| `remove_element` | Delete element | `id` (name) |

---

## Design Philosophy — THE IRON LAW

These are not suggestions. These are absolute rules. Violating any one of them means the model has failed and must be redone.

### "If it looks like a box with decoration, DELETE IT AND START OVER."

The formed model is the machine's IDENTITY. Players build ugly casing structures and expect a dramatic visual transformation. If the formed model does not impress, the entire multiblock system has failed. The moment a structure forms, the player should feel the machine "come alive" — from plain blocks to an imposing industrial apparatus.

### "Every machine is a character."

It has a face (front), a back, two sides, and a hat (top). **None of them should look the same.** A machine with four identical side faces is not a machine — it is a box wearing a costume.

- **Front**: The operator face. Control panel, indicator lights, access port, display screen.
- **Back**: The infrastructure face. Pipe connections, cable routing, exhaust ports, utility manifolds.
- **Left/Right**: The service faces. Ventilation grates, intake manifolds, structural framing, inspection hatches. These two may share a theme but must differ in specifics.
- **Top**: NEVER flat. Exhaust stacks, vents, pipes, antenna, cooling fins — elements at different heights creating an asymmetric skyline.
- **Bottom**: Heavy base/pedestal, always wider than the main body.

### "Silhouette tells the story."

If you show someone only the black silhouette of the machine (no texture, no color), they should be able to guess what it does. A crusher has a heavy jaw. A reactor has a containment chamber. A refinery has distillation columns. If the silhouette is a rectangle, you have failed.

### Concrete Ratios (Non-negotiable)

| Rule | Value |
|------|-------|
| Base width vs body width | Base extends 4-8px beyond body on each side |
| Shell panel coverage | **50-60% max** of any face area — the rest is gaps, recesses, grates |
| Surface NOT covered by shell | **At least 30%** must be recessed, cut away, or open |
| Minimum gap between shell panels | 4px |
| Gap between frame and shell | 2-4px visible |
| Major openings per model | At least 2 faces with openings 8-16px wide |
| Minimum protruding elements | 4+ (exhausts, pipes, manifolds, control panels) |
| Minimum visually distinct faces | 3 of 4 side faces must be clearly different |
| Recess depth for chambers | 6-10px minimum (dark interior visible) |
| Elements at different heights on top | At least 3 distinct height levels |

---

## Machine Identity Guidelines

Every multiblock must have SIGNATURE elements that communicate its function at a glance. Before building geometry, decide what the machine's "story" is.

### Identity Archetypes

| Machine Type | Signature Elements | Silhouette Defining Feature |
|---|---|---|
| **Reactor/Extractor** | Visible reaction chamber (deep recess with glow), gas output stacks, fluid intake manifold, containment frame | Tall central chamber with flanking stacks |
| **Crusher/Grinder** | Heavy jaw or mandible mechanism, ore hopper on top, discharge chute, thick base | Wide heavy bottom, asymmetric jaw shape |
| **Refinery/Distillery** | Tall thin distillation columns, connecting pipes between sections, tank drums | Multiple vertical elements at different heights |
| **Generator/Turbine** | Intake vents, exhaust ports, cooling fins, spinning element housing | Blocky base with prominent vent arrays |
| **Smelter/Furnace** | Chimney stack, loading door, slag port, heat radiator fins | Dominant chimney, wide firebox base |
| **Assembler/Fabricator** | Robotic arm housing, component hoppers, precision rail guides | Boxy with internal mechanism visible through windows |

For any machine not listed, ask: "What would this machine look like in a real factory?" Then identify 3-4 signature elements.

---

## Interactive Checkpoints (MANDATORY)

This workflow has **mandatory user checkpoints** where you MUST stop and wait for feedback. These are marked with `⏸️ CHECKPOINT`. At each checkpoint:

1. **Show the user** screenshots and describe what you've built
2. **Ask explicitly** if they want changes or are satisfied
3. **DO NOT proceed** until the user confirms
4. **Iterate** as many times as the user wants

The formed model IS the machine's visual identity — rushing it produces mediocre results. Take time, get feedback, iterate.

---

## Workflow — 5 Phases

1. **MUST: Get machine context** — Call `mcp__retronism-mod-maker__export_model_context`. The `<modelSize>` tag gives the FULL structure dimensions in pixels (e.g., width="48" height="64" depth="48" for a 3x4x3). **Use these as your coordinate space.**
2. **MUST: Create model in Blockbench** — Follow the Build Sequence below (Phases A→D).
3. **MUST: Export from Blockbench** — Export the model as JSON.
4. **MUST: Import into mod** — Call `mcp__retronism-mod-maker__import_model`.
5. **THEN: Generate the `FORMED_PARTS` Java array** from the Blockbench model elements.

---

## Complete Build Sequence (FOLLOW THIS EXACTLY)

This reference build shows a **3x3x4 Industrial Reactor** (48x64x48 pixels). Adapt dimensions from your `export_model_context` result.

### Phase A: Project Setup (4 tool calls)

```
Step 1: Create project
→ mcp__blockbench__create_project
  name: "{MachineName}"
  format: "bedrock_block"

Step 2: Create base texture (DARK base — NOT gray)
→ mcp__blockbench__create_texture
  name: "{machine_name}_texture"
  width: 16
  height: 16
  fill_color: "#505050"
  layer_name: "base"

Step 3: Create layer groups (4 groups — build inside out)
→ mcp__blockbench__add_group
  name: "core"
  origin: [24, 0, 24]
  rotation: [0, 0, 0]

→ mcp__blockbench__add_group
  name: "frame"
  origin: [24, 0, 24]
  rotation: [0, 0, 0]

→ mcp__blockbench__add_group
  name: "shell"
  origin: [24, 0, 24]
  rotation: [0, 0, 0]

→ mcp__blockbench__add_group
  name: "details"
  origin: [24, 0, 24]
  rotation: [0, 0, 0]
```

### Phase B: Structural Geometry (35-60 tool calls)

Build from inside out in 4 layers. **Every `place_cube` call MUST include `faces: true`, `texture`, and `group`.**

Coordinates span the FULL structure: 0-48 on X/Z, 0-64 on Y for a 3x4x3.

**CRITICAL: Before placing a single cube, sketch out the machine's silhouette mentally:**
- What makes this machine recognizable from its outline alone?
- Where are the major openings and recesses?
- Which face is the "operator face" (front) and which is the "infrastructure face" (back)?
- What elements break the rectangular profile?

#### Layer 1: Core Machinery (the functional heart — visible through gaps)

The core is what gives the machine its soul. It MUST be visible through shell gaps. Build elements that communicate the machine's function — a reaction chamber, a crushing mechanism, a turbine housing.

```
Step 4: Primary processing chamber (deep, visible through shell gaps)
→ mcp__blockbench__place_cube
  elements: [{name: "reaction_chamber", from: [14, 10, 14], to: [34, 50, 34]}]
  group: "core"
  texture: "{machine_name}_texture"
  faces: true

Step 5: Internal pipe manifold (connects chamber to exterior)
→ mcp__blockbench__place_cube
  elements: [
    {name: "core_pipe_left", from: [10, 22, 20], to: [14, 26, 28]},
    {name: "core_pipe_right", from: [34, 22, 20], to: [38, 26, 28]},
    {name: "core_pipe_bottom", from: [20, 4, 20], to: [28, 10, 28]},
    {name: "core_feed_tube", from: [22, 50, 22], to: [26, 56, 26]}
  ]
  group: "core"
  texture: "{machine_name}_texture"
  faces: true

Step 6: Internal cross-braces (structural support for chamber)
→ mcp__blockbench__place_cube
  elements: [
    {name: "brace_x", from: [10, 28, 22], to: [38, 32, 26]},
    {name: "brace_z", from: [22, 28, 10], to: [26, 32, 38]}
  ]
  group: "core"
  texture: "{machine_name}_texture"
  faces: true
```

#### Layer 2: Structural Frame (DOMINANT visual element — dark, heavy, industrial)

The frame defines the machine's skeleton. It should be the single most prominent visual element. Frame beams should be 4-6px thick (not 2-3px). The frame color (#303030 to #404040) should be the dominant color of the model.

```
Step 7: Corner pillars — heavy 6px columns (place one, duplicate 3)
→ mcp__blockbench__place_cube
  elements: [{name: "pillar_fl", from: [0, 0, 0], to: [6, 64, 6]}]
  group: "frame"
  texture: "{machine_name}_texture"
  faces: true

→ mcp__blockbench__duplicate_element
  id: "pillar_fl"
  newName: "pillar_fr"
  offset: [42, 0, 0]

→ mcp__blockbench__duplicate_element
  id: "pillar_fl"
  newName: "pillar_bl"
  offset: [0, 0, 42]

→ mcp__blockbench__duplicate_element
  id: "pillar_fl"
  newName: "pillar_br"
  offset: [42, 0, 42]

Step 8: Bottom frame rails (heavy base beams — 6px tall)
→ mcp__blockbench__place_cube
  elements: [
    {name: "rail_front_bot", from: [6, 0, 0], to: [42, 6, 6]},
    {name: "rail_back_bot", from: [6, 0, 42], to: [42, 6, 48]},
    {name: "rail_left_bot", from: [0, 0, 6], to: [6, 6, 42]},
    {name: "rail_right_bot", from: [42, 0, 6], to: [48, 6, 42]}
  ]
  group: "frame"
  texture: "{machine_name}_texture"
  faces: true

Step 9: Mid-height horizontal beams (structural cross-members — 4px tall)
→ mcp__blockbench__place_cube
  elements: [
    {name: "beam_front_mid", from: [6, 30, 0], to: [42, 34, 5]},
    {name: "beam_back_mid", from: [6, 30, 43], to: [42, 34, 48]},
    {name: "beam_left_mid", from: [0, 30, 6], to: [5, 34, 42]},
    {name: "beam_right_mid", from: [43, 30, 6], to: [48, 34, 42]}
  ]
  group: "frame"
  texture: "{machine_name}_texture"
  faces: true

Step 10: Top frame beams
→ mcp__blockbench__place_cube
  elements: [
    {name: "beam_front_top", from: [6, 58, 0], to: [42, 62, 5]},
    {name: "beam_back_top", from: [6, 58, 43], to: [42, 62, 48]},
    {name: "beam_left_top", from: [0, 58, 6], to: [5, 62, 42]},
    {name: "beam_right_top", from: [43, 58, 6], to: [48, 62, 42]}
  ]
  group: "frame"
  texture: "{machine_name}_texture"
  faces: true
```

#### Layer 3: Outer Shell (panels cover 50-60% MAX — rest is open)

Shell panels sit BETWEEN frame beams with 4px gaps between panels. Each face gets DIFFERENT treatment. Panels are 2-3px thick and sit 1-2px proud of the frame or recessed behind it.

**FRONT FACE (operator face) — control panel area, access port, indicator window:**
```
Step 11: Front lower panel (partial coverage — leaves access opening)
→ mcp__blockbench__place_cube
  elements: [
    {name: "panel_front_lower_left", from: [6, 6, 1], to: [20, 26, 3]},
    {name: "panel_front_lower_right", from: [28, 6, 1], to: [42, 26, 3]}
  ]
  group: "shell"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: 8px gap in the center between panels — shows core internals)

Step 12: Front upper panel (smaller — leaves room for control panel detail)
→ mcp__blockbench__place_cube
  elements: [
    {name: "panel_front_upper", from: [10, 38, 1], to: [38, 54, 3]}
  ]
  group: "shell"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: 4px gap below mid-beam, 4px gap above to top-beam — frame visible)
```

**BACK FACE (infrastructure face) — mostly open for pipe access:**
```
Step 13: Back panels (minimal — infrastructure face stays mostly open)
→ mcp__blockbench__place_cube
  elements: [
    {name: "panel_back_upper_left", from: [6, 38, 45], to: [22, 54, 47]},
    {name: "panel_back_upper_right", from: [26, 38, 45], to: [42, 54, 47]}
  ]
  group: "shell"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: Back lower section completely OPEN — pipes and core visible. 4px gap between panels.)
```

**LEFT FACE (service face) — ventilation grates:**
```
Step 14: Left side ventilation grate bars (thin horizontal bars with gaps)
→ mcp__blockbench__place_cube
  elements: [
    {name: "grate_left_1", from: [1, 8, 10], to: [3, 10, 38]},
    {name: "grate_left_2", from: [1, 14, 10], to: [3, 16, 38]},
    {name: "grate_left_3", from: [1, 20, 10], to: [3, 22, 38]},
    {name: "grate_left_4", from: [1, 38, 10], to: [3, 40, 38]},
    {name: "grate_left_5", from: [1, 44, 10], to: [3, 46, 38]},
    {name: "grate_left_6", from: [1, 50, 10], to: [3, 52, 38]}
  ]
  group: "shell"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: 2px bars, 4px gaps between them — frame and core visible through gaps)

Step 15: Left upper solid panel (above grate section)
→ mcp__blockbench__place_cube
  elements: [
    {name: "panel_left_upper", from: [1, 54, 8], to: [3, 58, 40]}
  ]
  group: "shell"
  texture: "{machine_name}_texture"
  faces: true
```

**RIGHT FACE (service face — different from left) — intake manifold area + partial panels:**
```
Step 16: Right side panels (partial coverage with large opening for intake)
→ mcp__blockbench__place_cube
  elements: [
    {name: "panel_right_lower", from: [45, 6, 6], to: [47, 26, 18]},
    {name: "panel_right_upper", from: [45, 38, 6], to: [47, 58, 42]}
  ]
  group: "shell"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: Large open section from z:18 to z:42 at lower level — intake area)
```

**TOP (never flat — multi-level elements):**
```
Step 17: Top shell (does NOT cover entire top — has openings)
→ mcp__blockbench__place_cube
  elements: [
    {name: "top_plate_rear", from: [6, 62, 24], to: [42, 64, 42]},
    {name: "top_plate_front_left", from: [6, 62, 6], to: [20, 64, 20]},
    {name: "top_plate_front_right", from: [28, 62, 6], to: [42, 64, 20]}
  ]
  group: "shell"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: Opening between front plates and gap between front/rear sections — not a sealed top)
```

**BASE (wider than body — creates grounded industrial feel):**
```
Step 18: Heavy base platform (extends 2px beyond structure on each side)
→ mcp__blockbench__place_cube
  elements: [
    {name: "base_platform", from: [-2, 0, -2], to: [50, 4, 50]},
    {name: "base_step", from: [0, 4, 0], to: [48, 6, 48]}
  ]
  group: "shell"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: base_platform is 52px wide vs 48px body = 2px overhang per side. base_step creates a stepped profile.)
```

#### Layer 4: External Details (the character-defining elements — MAKE OR BREAK)

This layer transforms a "structure" into a "machine." Without strong details, you have geometry but no character. Every detail must serve the machine's identity.

**Exhaust stacks (asymmetric — different heights create interesting skyline):**
```
Step 19: Primary exhaust stack (tall, rear-left)
→ mcp__blockbench__place_cube
  elements: [{name: "exhaust_main", from: [8, 64, 32], to: [14, 76, 38]}]
  group: "details"
  texture: "{machine_name}_texture"
  faces: true

Step 20: Secondary exhaust (shorter, rear-right)
→ mcp__blockbench__place_cube
  elements: [{name: "exhaust_secondary", from: [34, 64, 32], to: [40, 70, 38]}]
  group: "details"
  texture: "{machine_name}_texture"
  faces: true

Step 21: Exhaust caps (wider than stacks — industrial mushroom tops)
→ mcp__blockbench__place_cube
  elements: [
    {name: "exhaust_cap_main", from: [6, 76, 30], to: [16, 78, 40]},
    {name: "exhaust_cap_secondary", from: [32, 70, 30], to: [42, 72, 40]}
  ]
  group: "details"
  texture: "{machine_name}_texture"
  faces: true
```

**Intake manifold (protruding from right side — where the machine breathes):**
```
Step 22: Intake manifold housing (protruding funnel)
→ mcp__blockbench__place_cube
  elements: [
    {name: "intake_housing", from: [48, 12, 20], to: [54, 28, 36]},
    {name: "intake_throat", from: [46, 14, 22], to: [48, 26, 34]},
    {name: "intake_lip", from: [54, 10, 18], to: [56, 30, 38]}
  ]
  group: "details"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: Protrudes 8px beyond structure boundary — dramatic profile change on right side)
```

**Control panel (front face — the operator interface):**
```
Step 23: Control panel housing (protruding from front, at operator height)
→ mcp__blockbench__place_cube
  elements: [
    {name: "control_panel_frame", from: [14, 36, -3], to: [34, 50, 1]},
    {name: "control_panel_screen", from: [16, 40, -4], to: [32, 48, -3]},
    {name: "control_panel_shelf", from: [14, 34, -4], to: [34, 36, 1]}
  ]
  group: "details"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: Protrudes 4px from front face. Screen is recessed 1px into frame for depth.)
```

**Pipe runs along exterior (connecting elements visually):**
```
Step 24: External pipe runs (connect exhaust area to base)
→ mcp__blockbench__place_cube
  elements: [
    {name: "pipe_vertical_back", from: [44, 6, 36], to: [47, 62, 40]},
    {name: "pipe_horizontal_top", from: [14, 60, 34], to: [44, 62, 37]},
    {name: "pipe_elbow", from: [44, 58, 34], to: [47, 62, 37]}
  ]
  group: "details"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: Pipes run along the OUTSIDE of the shell, connecting different sections)
```

**Cooling fins on left side (between grate bars — functional detail):**
```
Step 25: Cooling fin housings
→ mcp__blockbench__place_cube
  elements: [
    {name: "fin_housing_upper", from: [-2, 38, 14], to: [1, 52, 34]},
    {name: "fin_housing_lower", from: [-2, 8, 14], to: [1, 22, 34]}
  ]
  group: "details"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: Protrude 2px from left face, aligned with grate sections)
```

**Support brackets and base reinforcement:**
```
Step 26: L-shaped support brackets (corner reinforcements)
→ mcp__blockbench__place_cube
  elements: [
    {name: "bracket_fl_h", from: [-2, 4, -2], to: [8, 8, 4]},
    {name: "bracket_fl_v", from: [-2, 4, -2], to: [4, 16, 4]},
    {name: "bracket_fr_h", from: [40, 4, -2], to: [50, 8, 4]},
    {name: "bracket_fr_v", from: [44, 4, -2], to: [50, 16, 4]},
    {name: "bracket_bl_h", from: [-2, 4, 44], to: [8, 8, 50]},
    {name: "bracket_bl_v", from: [-2, 4, 44], to: [4, 16, 50]},
    {name: "bracket_br_h", from: [40, 4, 44], to: [50, 8, 50]},
    {name: "bracket_br_v", from: [44, 4, 44], to: [50, 16, 50]}
  ]
  group: "details"
  texture: "{machine_name}_texture"
  faces: true
  (NOTE: L-shaped brackets at each corner — extend beyond base, add visual weight)
```

**Vent stack on front-left top (breaks skyline asymmetry):**
```
Step 27: Vent stack with hood
→ mcp__blockbench__place_cube
  elements: [
    {name: "vent_stack", from: [8, 64, 8], to: [14, 68, 14]},
    {name: "vent_hood", from: [6, 68, 6], to: [16, 70, 16]}
  ]
  group: "details"
  texture: "{machine_name}_texture"
  faces: true
```

That is ~50 elements organized in 4 layers, demonstrating:
- **Unique silhouette**: Exhaust stacks at different heights, intake manifold protruding right, control panel protruding front, cooling fins on left, vent stack front-left — NO rectangular outline
- **Deep recesses**: Front center 8px gap shows core, back lower section completely open, left side grate bars with visible interior
- **Face differentiation**: Front=control panel + split panels, Back=mostly open infrastructure, Left=ventilation grates + cooling fins, Right=intake manifold + partial panels
- **Heavy base**: Platform extends beyond body, L-shaped brackets at corners
- **Asymmetric top**: Main exhaust (12px tall rear-left), secondary exhaust (6px tall rear-right), vent hood (front-left) — 3 different heights
- **Core visible**: Through front gap, back opening, left grate gaps, top openings
- **Shell coverage ~50%**: Large sections of each face are open, grated, or recessed

**Adapt this level of detail and variety for every multiblock.**

### ── QUALITY GATE 1: Geometry & Silhouette Check ──

```
→ mcp__blockbench__set_camera_angle
  position: [80, 60, 80]
  target: [24, 32, 24]
  projection: "perspective"

→ mcp__blockbench__capture_screenshot

→ mcp__blockbench__set_camera_angle
  position: [-80, 40, -20]
  target: [24, 32, 24]
  projection: "perspective"

→ mcp__blockbench__capture_screenshot

EVALUATE (you MUST answer ALL of these honestly before proceeding):
  1. SILHOUETTE: "Could someone identify this machine type from silhouette alone?" — If NO, add signature elements.
  2. RECESSES: "Are there recesses at least 6px deep where you see darkness/internal elements?" — If NO, deepen openings or remove shell panels.
  3. SHELL COVERAGE: "Is at least 30% of total surface area NOT covered by shell panels?" — If NO, remove panels, add grates, create openings.
  4. BASE WEIGHT: "Does the base look visually heavier and wider than the body?" — If NO, extend base, add brackets.
  5. FACE VARIETY: "Are at least 3 of 4 side faces visually distinct?" — If NO, differentiate faces (add grates to one, opening to another, protrusion to third).
  6. TOP PROFILE: "Are there at least 3 elements at different heights on top?" — If NO, add stacks, vents, pipes at varying heights.
  7. CORE VISIBILITY: "Can you see internal elements through at least 2 gaps in the shell?" — If NO, enlarge gaps or add more core elements.
  8. BOX TEST: "Does this look like a MACHINE or a DECORATED BOX?" — If BOX, start over. Seriously.

If ANY answer fails → fix it before showing the user. Do NOT rationalize passing a failed check.
```

#### ⏸️ CHECKPOINT: Geometry Review
After passing your self-evaluation, show the screenshot(s) to the user.
- Ask: **"Essa é a forma base da estrutura formed. O que acha? Quer que eu adicione mais detalhes, mude proporções, adicione/remova elementos?"**
- **Iterate** — add/remove/modify elements based on feedback, take new screenshots, show again.
- Only proceed to texturing when the user says the geometry is good.

### Phase C: Texture Painting (10-20 tool calls)

Same 16x16 texture shared across the entire structure. Paint it with industrial detail.

```
Step 20: Paint color zones (DARK INDUSTRIAL palette)
→ mcp__blockbench__draw_shape_tool
  shape: "rectangle"
  start: {x: 0, y: 0}
  end: {x: 16, y: 6}
  color: "#383838"
  texture_id: "{machine_name}_texture"
  (dark charcoal — frame/beams zone, DOMINANT)

→ mcp__blockbench__draw_shape_tool
  shape: "rectangle"
  start: {x: 0, y: 6}
  end: {x: 16, y: 12}
  color: "#606060"
  texture_id: "{machine_name}_texture"
  (medium-dark — body panels zone)

→ mcp__blockbench__draw_shape_tool
  shape: "rectangle"
  start: {x: 0, y: 12}
  end: {x: 16, y: 14}
  color: "#707070"
  texture_id: "{machine_name}_texture"
  (medium gray — top/lighter surfaces)

→ mcp__blockbench__draw_shape_tool
  shape: "rectangle"
  start: {x: 0, y: 14}
  end: {x: 16, y: 16}
  color: "#B87333"
  texture_id: "{machine_name}_texture"
  (accent strip — machine-specific color, SPARINGLY)

Step 21: Paint panel seams (MAX 2 horizontal + 2 vertical — rely on geometry for detail, not texture)
→ mcp__blockbench__paint_with_brush
  coordinates: [{x:0,y:6}, {x:15,y:6}]
  brush_settings: {color: "#2A2A2A", size: 1, opacity: 255}
  connect_strokes: true
  texture_id: "{machine_name}_texture"
  (seam between frame and body zones)

→ mcp__blockbench__paint_with_brush
  coordinates: [{x:0,y:12}, {x:15,y:12}]
  brush_settings: {color: "#2A2A2A", size: 1, opacity: 255}
  connect_strokes: true
  texture_id: "{machine_name}_texture"
  (seam between body and accent zones)

→ mcp__blockbench__paint_with_brush
  coordinates: [{x:8,y:0}, {x:8,y:15}]
  brush_settings: {color: "#2A2A2A", size: 1, opacity: 255}
  connect_strokes: true
  texture_id: "{machine_name}_texture"
  (single vertical division)

Step 22: Paint rivets at key intersections ONLY (4 max — less is more)
→ mcp__blockbench__paint_with_brush
  coordinates: [
    {x:8,y:6}, {x:8,y:12},
    {x:4,y:6}, {x:12,y:6}
  ]
  brush_settings: {color: "#222222", size: 1, opacity: 255}
  texture_id: "{machine_name}_texture"

Step 23: Paint edge highlights (subtle — dark palette means highlights are medium gray, NOT white)
→ mcp__blockbench__paint_with_brush
  coordinates: [{x:0,y:0}, {x:15,y:0}]
  brush_settings: {color: "#505050", size: 1, opacity: 200}
  connect_strokes: true
  texture_id: "{machine_name}_texture"
  (top edge of frame zone — just slightly lighter than frame)

→ mcp__blockbench__paint_with_brush
  coordinates: [{x:0,y:7}, {x:15,y:7}]
  brush_settings: {color: "#707070", size: 1, opacity: 180}
  connect_strokes: true
  texture_id: "{machine_name}_texture"
  (top edge of body zone)

Step 24: Paint edge shadows
→ mcp__blockbench__paint_with_brush
  coordinates: [{x:0,y:5}, {x:15,y:5}]
  brush_settings: {color: "#2A2A2A", size: 1, opacity: 200}
  connect_strokes: true
  texture_id: "{machine_name}_texture"

→ mcp__blockbench__paint_with_brush
  coordinates: [{x:0,y:11}, {x:15,y:11}]
  brush_settings: {color: "#404040", size: 1, opacity: 200}
  connect_strokes: true
  texture_id: "{machine_name}_texture"

Step 25: Paint indicator pops (TINY bright accents against dark background — high contrast)
→ mcp__blockbench__paint_with_brush
  coordinates: [{x:10,y:8}]
  brush_settings: {color: "#D4760A", size: 1, opacity: 255}
  texture_id: "{machine_name}_texture"
  (single orange indicator pixel — stands out against dark body)

→ mcp__blockbench__paint_with_brush
  coordinates: [{x:6,y:8}]
  brush_settings: {color: "#4A90D9", size: 1, opacity: 255}
  texture_id: "{machine_name}_texture"
  (single blue port indicator)

Step 26: Add subtle metal noise (darker AND lighter scattered pixels)
→ mcp__blockbench__paint_with_brush
  coordinates: [{x:2,y:2}, {x:5,y:3}, {x:11,y:1}, {x:14,y:4}]
  brush_settings: {color: "#454545", size: 1, opacity: 100}
  texture_id: "{machine_name}_texture"
  (slightly lighter noise on frame zone)

→ mcp__blockbench__paint_with_brush
  coordinates: [{x:1,y:8}, {x:6,y:9}, {x:13,y:7}, {x:3,y:10}]
  brush_settings: {color: "#6E6E6E", size: 1, opacity: 100}
  texture_id: "{machine_name}_texture"
  (slightly lighter noise on body zone)

→ mcp__blockbench__paint_with_brush
  coordinates: [{x:4,y:9}, {x:11,y:10}]
  brush_settings: {color: "#4A3A1A", size: 1, opacity: 60}
  texture_id: "{machine_name}_texture"
  (subtle rust spots)

Step 27: Warning hazard stripe on accent row (optional — use when machine has danger/power element)
→ mcp__blockbench__paint_with_brush
  coordinates: [{x:0,y:15}, {x:2,y:15}, {x:4,y:15}, {x:6,y:15}, {x:8,y:15}, {x:10,y:15}, {x:12,y:15}, {x:14,y:15}]
  brush_settings: {color: "#D4A017", size: 1, opacity: 255}
  texture_id: "{machine_name}_texture"

→ mcp__blockbench__paint_with_brush
  coordinates: [{x:1,y:15}, {x:3,y:15}, {x:5,y:15}, {x:7,y:15}, {x:9,y:15}, {x:11,y:15}, {x:13,y:15}, {x:15,y:15}]
  brush_settings: {color: "#252525", size: 1, opacity: 255}
  texture_id: "{machine_name}_texture"
```

### ── QUALITY GATE 2: Texture Check ──

```
→ mcp__blockbench__capture_app_screenshot
  (shows texture flat in UV editor)

EVALUATE BRUTALLY (you MUST answer ALL before proceeding):
  1. Is the DOMINANT color darker than #606060? (the model should read as DARK)
  2. Can you clearly distinguish frame zones from panel zones from accent? (3 distinct values)
  3. Are panel seam lines visible but NOT overwhelming? (max 2H + 2V lines)
  4. Is there at least one bright accent pop (indicator/warning) against the dark background?
  5. Would this texture look right next to an Immersive Engineering machine?

If ANY answer is NO → repaint. A light/washed-out texture ruins the entire model.
```

### Phase D: UV Mapping Refinement

Map each element group to the correct texture zone. The mapping creates visual hierarchy — frame reads dark, body reads medium, top reads lighter.

```
Step 29: Map ALL frame elements to dark zone (rows 0-5)
→ mcp__blockbench__modify_cube
  id: "pillar_fl"
  uv_offset: [0, 0]

(repeat for: pillar_fr, pillar_bl, pillar_br, ALL rail_* elements, ALL beam_* elements, base_platform, base_step, ALL bracket_* elements)

Step 30: Map shell panels and core to body zone (rows 6-11)
→ mcp__blockbench__modify_cube
  id: "panel_front_lower_left"
  uv_offset: [0, 4]

(repeat for: ALL panel_* elements, ALL grate_* elements, reaction_chamber, ALL core_pipe_* elements, ALL brace_* elements)

Step 31: Map top surfaces and upper details to lighter zone (rows 10-14)
→ mcp__blockbench__modify_cube
  id: "top_plate_rear"
  uv_offset: [0, 10]

(repeat for: ALL top_plate_* elements, vent_stack, vent_hood)

Step 32: Map accent elements to accent zone (row 15 — used VERY sparingly)
→ mcp__blockbench__modify_cube
  id: "control_panel_screen"
  uv_offset: [0, 13]

(repeat for: control_panel_screen ONLY — accent on 2-4 pixels max of final model)

Step 33: Map exhaust stacks and pipes to frame zone (dark — industrial infrastructure)
→ mcp__blockbench__modify_cube
  id: "exhaust_main"
  uv_offset: [0, 0]

(repeat for: exhaust_secondary, exhaust_cap_*, ALL pipe_* elements, intake_housing, intake_throat)

Step 34: Map intake lip to accent zone (draws the eye to functional element)
→ mcp__blockbench__modify_cube
  id: "intake_lip"
  uv_offset: [0, 13]
```

### ── QUALITY GATE 3: Final Model Verification ──

Take screenshots from ALL 4 angles plus top-down:

```
→ mcp__blockbench__set_camera_angle
  position: [80, 50, 80], target: [24, 32, 24], projection: "perspective"
→ mcp__blockbench__capture_screenshot
  (front-right 3/4 view)

→ mcp__blockbench__set_camera_angle
  position: [-80, 50, -80], target: [24, 32, 24], projection: "perspective"
→ mcp__blockbench__capture_screenshot
  (back-left 3/4 view)

→ mcp__blockbench__set_camera_angle
  position: [80, 50, -80], target: [24, 32, 24], projection: "perspective"
→ mcp__blockbench__capture_screenshot
  (front-left 3/4 view — shows left service face)

→ mcp__blockbench__set_camera_angle
  position: [-80, 50, 80], target: [24, 32, 24], projection: "perspective"
→ mcp__blockbench__capture_screenshot
  (back-right 3/4 view — shows right service face + intake)

→ mcp__blockbench__set_camera_angle
  position: [0, 100, 0], target: [24, 32, 24], projection: "perspective"
→ mcp__blockbench__capture_screenshot
  (top-down — shows skyline elements)

EVALUATE — THE FINAL JUDGMENT (answer ALL honestly):
  1. DECORATED BOX TEST: "Does this look like a MACHINE or a DECORATED BOX?" — If there is ANY hesitation, it is a box. Fix it.
  2. INTERNAL VISIBILITY: "From each angle, can you see internal elements (core, pipes) through gaps in the shell?" — If NO from any angle → enlarge gaps on that face.
  3. FACE DIFFERENTIATION: "Looking at each screenshot, are the visible faces clearly different from each other?" — If any two faces look similar → redesign one.
  4. FRAME DOMINANCE: "Is the dark frame the most prominent visual element (not the shell panels)?" — If panels dominate → they cover too much, remove some.
  5. SILHOUETTE IDENTITY: "From the front-right 3/4 view, could you guess the machine type?" — If NO → add more signature elements.
  6. IMMERSIVE ENGINEERING STANDARD: "Would this look at home in an Immersive Engineering factory hall?" — If NO → it is too light, too clean, too simple, or too boxy.
  7. WOW FACTOR: "When a player's multiblock forms and this model appears, will they feel the 'transformation moment'?" — If NO → the model lacks drama. Add height variation, deepen recesses, increase protrusions.

If ANY answer fails → go back and fix. Do NOT compromise on quality. The model is the mod's visual flagship.
```

#### ⏸️ CHECKPOINT: Final Model Review (MOST IMPORTANT)
Show the user screenshots from at least 3 angles (front-right, back-left, and top-down minimum).
- Ask: **"Modelo formed finalizado com textura. O que acha? Quer ajustes na textura, na forma, nas cores, ou está aprovado para exportar?"**
- **This is the last chance to iterate.** Be patient — the user may want several rounds of changes.
- Only proceed to export when the user explicitly says it's good / approved / "pode exportar".

### Phase E: Export and Integration

After passing all 3 quality gates AND receiving user approval:
1. Save the Blockbench project
2. Export as JSON and save to `src/retronism/assets/models/`
3. Call `mcp__retronism-mod-maker__import_model` with the JSON
4. Generate the FORMED_PARTS array and render class (see Integration section below)

---

## Texture Painting Recipes (Reusable Patterns — DARK PALETTE)

### Recipe: Panel Seams (SIMPLIFIED — max 2H + 1V)
```
// Horizontal zone separators
paint_with_brush: coords [{x:0,y:6},{x:15,y:6}], color="#2A2A2A", size=1, connect_strokes=true
paint_with_brush: coords [{x:0,y:12},{x:15,y:12}], color="#2A2A2A", size=1, connect_strokes=true
// Single vertical division
paint_with_brush: coords [{x:8,y:0},{x:8,y:15}], color="#2A2A2A", size=1, connect_strokes=true
```
NOTE: Do NOT add 6 panel lines like a grid. Rely on 3D GEOMETRY for visual complexity, not texture.

### Recipe: Rivets (4 MAX — at key intersections only)
```
paint_with_brush: coords [{x:8,y:6},{x:8,y:12},{x:4,y:6},{x:12,y:6}], color="#222222", size=1
```

### Recipe: Indicator Pops (tiny bright pixels against dark background)
```
// Orange indicator (1 pixel)
paint_with_brush: coords [{x:10,y:8}], color="#D4760A", size=1, opacity=255
// Blue port indicator (1 pixel)
paint_with_brush: coords [{x:6,y:8}], color="#4A90D9", size=1, opacity=255
```
These TINY pops create more visual interest than large colored zones. Less is more.

### Recipe: Brushed Metal Noise (SUBTLE — adapted for dark palette)
```
// Slightly lighter noise on dark zones
paint_with_brush: coords [{x:2,y:2},{x:5,y:3},{x:11,y:1},{x:14,y:4}], color="#454545", size=1, opacity=100
// Slightly lighter noise on body zones
paint_with_brush: coords [{x:1,y:8},{x:6,y:9},{x:13,y:7},{x:3,y:10}], color="#6E6E6E", size=1, opacity=100
```

### Recipe: Warning Hazard Stripe
```
paint_with_brush: coords [{x:0,y:15},{x:2,y:15},{x:4,y:15},{x:6,y:15},{x:8,y:15},{x:10,y:15},{x:12,y:15},{x:14,y:15}], color="#D4A017", size=1
paint_with_brush: coords [{x:1,y:15},{x:3,y:15},{x:5,y:15},{x:7,y:15},{x:9,y:15},{x:11,y:15},{x:13,y:15},{x:15,y:15}], color="#252525", size=1
```

### Recipe: Rust/Weathering Spots (SUBTLE)
```
paint_with_brush: coords [{x:4,y:9},{x:11,y:10}], color="#4A3A1A", size=1, opacity=60
paint_with_brush: coords [{x:7,y:8}], color="#3A2A10", size=1, opacity=40
```

---

## Scale Awareness

- Coordinates span the FULL structure: e.g., 0-48 on X/Z, 0-64 on Y for a 3x4x3
- Budget roughly 6-10 elements per block position
- Target 35-55 elements total depending on structure size
- Frame beams: 4-6px thick (not 2-3px like single-block models)
- Minimum gap between elements: 4px (smaller gaps are invisible at game rendering distance)
- Protrusions: 4-8px to be noticeable
- Recesses: 6-10px deep for visible chambers
- Base overhang: 2-8px per side beyond the structure boundary

---

## Geometric Techniques — with Tool Sequences

### Stepped Diagonals (slopes at large scale)
At multiblock scale, 1px steps across 48+ pixels create smooth-looking slopes.

**Tool sequence — tapered reactor top:**
```
place_cube: elements=[{name:"taper_1", from:[4,56,4], to:[44,58,44]}], group="shell", texture, faces=true
place_cube: elements=[{name:"taper_2", from:[8,58,8], to:[40,60,40]}], group="shell", texture, faces=true
place_cube: elements=[{name:"taper_3", from:[12,60,12], to:[36,62,36]}], group="shell", texture, faces=true
place_cube: elements=[{name:"taper_4", from:[16,62,16], to:[32,64,32]}], group="shell", texture, faces=true
```

### Structural Framing (industrial look)
Thick beams defining the shape, with panels between them.

**Tool sequence:**
```
place_cube: elements=[{name:"pillar", from:[0,0,0], to:[4,64,4]}], group="frame", texture, faces=true
duplicate_element: id="pillar", newName="pillar_2", offset=[44,0,0]
duplicate_element: id="pillar", newName="pillar_3", offset=[0,0,44]
duplicate_element: id="pillar", newName="pillar_4", offset=[44,0,44]
place_cube: elements=[{name:"crossbeam", from:[4,30,0], to:[44,34,4]}], group="frame", texture, faces=true
```

### Deep Recesses and Chambers
At multiblock scale, recesses can be 4-8px deep — creating visible chambers.

**Tool sequence:**
```
place_cube: elements=[{name:"outer_wall", from:[0,16,0], to:[48,48,4]}], group="shell", texture, faces=true
place_cube: elements=[{name:"chamber_recess", from:[8,20,4], to:[40,44,12]}], group="shell", texture, faces=true
place_cube: elements=[{name:"inner_machinery", from:[16,24,8], to:[32,40,10]}], group="core", texture, faces=true
```

### Protruding Industrial Elements
At this scale, protrusions become dramatic.

**Tool sequence:**
```
// Exhaust stacks
place_cube: elements=[{name:"exhaust", from:[12,64,12], to:[16,72,16]}], group="details", texture, faces=true
// Intake manifold
place_cube: elements=[{name:"intake", from:[-4,16,14], to:[0,28,34]}], group="details", texture, faces=true
// Pipe run along exterior
place_cube: elements=[{name:"pipe_run", from:[6,58,6], to:[42,60,8]}], group="details", texture, faces=true
```

### Ventilation Grates (Signature Industrial Element)
Thin horizontal bars (2px tall) with 4px gaps between them. The frame and core are visible through the gaps. This is one of the most important techniques for breaking up flat surfaces.

**Tool sequence:**
```
place_cube: elements=[
  {name:"grate_1", from:[1,8,10], to:[3,10,38]},
  {name:"grate_2", from:[1,14,10], to:[3,16,38]},
  {name:"grate_3", from:[1,20,10], to:[3,22,38]}
], group="shell", texture, faces=true
(2px bars, 4px gaps — shows dark interior between bars)
```

### Asymmetric Top Profile
The top of the machine should have elements at 3+ different heights. This creates a distinctive skyline that identifies the machine from a distance.

**Tool sequence:**
```
place_cube: elements=[{name:"stack_tall", from:[8,64,30], to:[14,76,36]}], group="details", texture, faces=true    // +12px above structure
place_cube: elements=[{name:"stack_short", from:[34,64,30], to:[40,70,36]}], group="details", texture, faces=true  // +6px above structure
place_cube: elements=[{name:"vent_hood", from:[6,64,6], to:[16,70,16]}], group="details", texture, faces=true      // +6px, different position
place_cube: elements=[{name:"vent_cap", from:[4,70,4], to:[18,72,18]}], group="details", texture, faces=true       // +8px with wider cap
```

### L-Shaped Corner Brackets
Heavy brackets at the base corners add visual weight and make the machine look bolted to the ground.

**Tool sequence:**
```
place_cube: elements=[
  {name:"bracket_fl_h", from:[-2,4,-2], to:[8,8,4]},
  {name:"bracket_fl_v", from:[-2,4,-2], to:[4,16,4]}
], group="details", texture, faces=true
// Duplicate for other 3 corners with appropriate offsets
```

### Negative Space and Gaps
At multiblock scale, gaps are ESSENTIAL — a solid mass looks like a blob:
- Leave 4px minimum gaps between shell panels (not 1-2px — invisible at game distance)
- Create visible internal chambers with 6-10px deep recesses
- Ventilation grates (thin horizontal bars with 4px gaps showing frame behind)
- At least 2 faces must have major openings (8-16px wide)

### Asymmetry with Purpose
Real industrial machines are NOT symmetrical boxes:
- Input side has intake manifold, output side has exhaust
- Front has control panel, back has pipe connections
- Top has varied roofline (exhausts on one side, vents on another)
- Left and right faces should have different treatment (grates vs panels vs openings)

---

## Texture Craft — Color Palette

### Texture Zone Layout (DARK INDUSTRIAL — Immersive Engineering Standard)
```
┌──────────────────────────────────────────────────────────────┐
│  TEXTURE ZONE LAYOUT (16x16 shared texture)                  │
│                                                              │
│  rows  0-5:  DARK FRAME    #383838  (dominant — pillars,     │
│              beams, base, infrastructure pipes, exhausts)     │
│  rows  6-11: MEDIUM BODY   #606060  (shell panels, core,    │
│              grate bars)                                      │
│  rows 12-14: LIGHTER TOP   #707070  (top plates, upper      │
│              details — still medium gray, NOT light)          │
│  row  15:    ACCENT         varies  (machine-specific,       │
│              used on 2-4 pixels max of the final model)      │
│                                                              │
│  HIERARCHY: dark frame DOMINANT > medium body > lighter top  │
│  The model should read as DARK with medium and light accents │
│  NOT as medium-gray with dark and light accents              │
└──────────────────────────────────────────────────────────────┘
```

### Color Values

| Element | Color Range | Notes |
|---------|------------|-------|
| Frame/structural | #303030 to #404040 | DOMINANT color. Dark charcoal. |
| Shell panels | #585858 to #686868 | Medium-dark. NOT #808080 (too light). |
| Top surfaces | #686868 to #787878 | Slightly lighter than panels. Still dark. |
| Base fill_color | #505050 | Starting point before zone painting. |
| Panel seams | #2A2A2A | Near-black lines between zones. |
| Rivets | #222222 | Darker than seams, at intersections. |
| Edge highlight | #707070 | Subtle — NOT bright. |
| Edge shadow | #404040 | Slightly darker than body. |
| Accent (copper) | #B87333 | 2-4 pixels max on final model. |
| Accent (industrial orange) | #D4760A | For hot/energy machines. |
| Accent (industrial blue) | #4A90D9 | For fluid/chemical machines. |
| Accent (warning yellow) | #D4A017 | For hazard stripes only. |
| Indicator green | #33CC55 | Single pixel on control panel. |
| Indicator red | #FF3333 | Single pixel for warnings. |
| NEVER use | #000000 pure black | |
| NEVER use | #FFFFFF pure white | |
| NEVER use | #808080 as base | Too light. Washed out. |
| NEVER use | #A8A8A8 anywhere | Way too light for industrial. |

---

## Technical Constraints (Beta 1.7.3)

### Boxes Only — No Rotations
Same as single-block: `setBlockBounds` + `renderStandardBlock`, axis-aligned boxes only.

**Coordinates for formed models** range from 0 to the full structure size in pixels. A 3x3x4 structure uses 0-48 on X, 0-64 on Y, 0-48 on Z. Elements CAN extend beyond these bounds (exhaust stacks above, base platform wider, protrusions beyond sides).

### Texture System
- Single 16x16 block texture, all faces reference `#0`
- `renderStandardBlock` handles lighting automatically
- Use UV offsets to select WHICH PART of the texture each face displays

### UV Rules
Same as single-block — UVs must be proportional to face dimensions. Never stretch. Use UV offsets to map different elements to different texture regions.

### Performance
- Target 35-55 elements (proportional to structure volume)
- Avoid hidden faces (boxes completely inside other boxes)
- Core elements should be partially occluded by shell, not fully hidden

---

## Integration with the Mod

### FORMED_PARTS Array
The full structure model, coordinates in structure-space pixels:

```java
private static final float[][] FORMED_PARTS = {
    // Structure-space pixel coordinates (e.g., 0-48 for 3-wide)
    // Can extend BEYOND structure bounds for protrusions
    {-2, 0, -2, 50, 4, 50},     // base_platform (wider than structure)
    {0, 4, 0, 48, 6, 48},       // base_step
    {0, 0, 0, 6, 64, 6},        // pillar_fl
    {42, 0, 0, 48, 64, 6},      // pillar_fr
    // ... all elements from Blockbench model
    {8, 64, 32, 14, 76, 38},    // exhaust_main (extends above)
    {48, 12, 20, 54, 28, 36},   // intake_housing (extends beyond side)
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
- Elements CAN have negative coordinates or exceed structure bounds (protrusions, wider base)
- The offset translates from structure origin to controller position
- The controller tile entity must store `structOffX/Y/Z` during `checkStructure`

### Inventory Render
Render as a normal standard block (plain cube) — the formed model is too large for an inventory slot.

### Casing Invisibility When Formed

Casing blocks must become invisible when the multiblock is formed:

**Approach A (simple, for 3x3x3 to 3x3x5):**
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

---

## Rules

- **ALWAYS stop at every ⏸️ CHECKPOINT and wait for user feedback — NEVER skip checkpoints**
- **NEVER proceed to the next phase without explicit user confirmation**
- **NEVER export a model without user approval** — always show screenshots and ask first
- **ALWAYS offer to iterate** — if the user suggests a change, make it, screenshot, and show again
- ALWAYS follow the Complete Build Sequence (Phases A→E) in order
- ALWAYS pass `faces: true`, `texture`, and `group` on every `place_cube` call
- ALWAYS call `export_model_context` first — the `<modelSize>` gives exact pixel dimensions
- ALWAYS organize elements into 4 groups: core, frame, shell, details
- ALWAYS use `duplicate_element` for repeating structural patterns (pillars, beams, exhausts)
- ALWAYS pass all 3 Quality Gates before exporting — take screenshots and evaluate honestly
- ALWAYS paint the texture with dark industrial palette (base fill #505050, frame #383838, body #606060)
- ALWAYS use UV offsets via `modify_cube` to map groups to different texture regions
- ALWAYS design each face differently (front=operator, back=infrastructure, sides=service, top=skyline)
- ALWAYS create a unique silhouette that communicates the machine's function
- ALWAYS include deep recesses (6px+) where internal elements are visible
- ALWAYS make the base wider/heavier than the body
- ALWAYS have at least 3 height levels on top (never flat)
- ALWAYS use at least 5 geometric techniques per model (framing + grates + recesses + protrusions + asymmetric top minimum)
- ALWAYS create visible layered composition (core → frame → shell → details)
- ALWAYS handle formed/unformed states in the render class
- ALWAYS make the casing renderer hide blocks when formed
- ALWAYS reset block bounds to 0-1 after rendering
- Coordinates MUST span the full structure dimensions (from `<modelSize>`)
- Shell panels MUST cover no more than 50-60% of any face
- At least 30% of surface area MUST be uncovered (recesses, grates, openings)
- At least 3 of 4 side faces MUST be visually distinct
- Frame MUST be the dominant visual element (darkest, thickest, most prominent)
- NEVER leave the model as a plain cube or decorated box
- NEVER make all faces look the same
- NEVER create a separate CONTROLLER_PARTS array — unformed controller is just a normal block
- NEVER leave UV mapping at default for all faces
- NEVER use flat single-color textures — minimum: 3 zones, seams, rivets, edge shading
- NEVER make a solid mass — use negative space, gaps, and visible internal elements
- NEVER use #808080 or lighter as the base/dominant color — too washed out
- NEVER use more than 4 rivets or 4 panel seam lines on the 16x16 texture
- NEVER export a model that fails any Quality Gate
- NEVER rationalize passing a failed quality check — if it fails, fix it
- Target 35-55 elements (proportional to structure volume)
- **If the model looks like a box with panels on it, DELETE EVERYTHING AND START OVER**
