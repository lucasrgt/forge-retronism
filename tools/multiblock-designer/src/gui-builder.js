// Minecraft-style GUI Builder for 176x166 textures
// Renders at 3x scale on canvas (528x498)

const SCALE = 3;
const GUI_W = 176;
const GUI_H = 166;

const COLORS = {
  BG:    [198, 198, 198],
  SD:    [55, 55, 55],
  SL:    [139, 139, 139],
  WH:    [255, 255, 255],
  DK:    [85, 85, 85],
  BAR_BG:[64, 64, 64],
  BK:    [0, 0, 0],
};

const COMP_DEFS = {
  slot:           { w: 18, h: 18, color: '#8b8b8b', label: 'Slot' },
  big_slot:       { w: 26, h: 26, color: '#7b7b7b', label: 'Big Slot' },
  energy_bar:     { w: 8,  h: 54, color: '#404040', label: 'Energy Bar' },
  progress_arrow: { w: 24, h: 17, color: '#686868', label: 'Arrow' },
  flame:          { w: 14, h: 14, color: '#c86400', label: 'Flame' },
  fluid_tank:     { w: 16, h: 54, color: '#4488ff', label: 'Fluid Tank' },
  gas_tank:       { w: 16, h: 54, color: '#aaaaaa', label: 'Gas Tank' },
  separator:      { w: 160, h: 2, color: '#555555', label: 'Separator' },
};

export class GuiBuilder {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.components = [];
    this.selectedIndex = -1;
    this.dragging = false;
    this.dragOffset = { x: 0, y: 0 };
    this.gridSnap = 2;

    canvas.addEventListener('mousedown', (e) => this.onMouseDown(e));
    canvas.addEventListener('mousemove', (e) => this.onMouseMove(e));
    canvas.addEventListener('mouseup', () => this.onMouseUp());
    canvas.addEventListener('contextmenu', (e) => { e.preventDefault(); this.onRightClick(e); });
  }

  toGuiCoords(e) {
    const rect = this.canvas.getBoundingClientRect();
    return {
      x: Math.floor((e.clientX - rect.left) / SCALE),
      y: Math.floor((e.clientY - rect.top) / SCALE),
    };
  }

  snap(v) {
    return Math.round(v / this.gridSnap) * this.gridSnap;
  }

  addComponent(type, x, y, opts = {}) {
    const def = COMP_DEFS[type];
    if (!def) return;
    this.components.push({
      type, x, y,
      w: opts.w || def.w,
      h: opts.h || def.h,
      slotType: opts.slotType || null,
    });
    this.render();
  }

  removeComponent(index) {
    this.components.splice(index, 1);
    if (this.selectedIndex >= this.components.length) this.selectedIndex = -1;
    this.render();
  }

  clear() {
    this.components = [];
    this.selectedIndex = -1;
    this.render();
  }

  loadPreset(name) {
    this.clear();
    switch (name) {
      case 'processor':
        this.addComponent('slot', 55, 34, { slotType: 'input' });
        this.addComponent('progress_arrow', 79, 35);
        this.addComponent('slot', 115, 34, { slotType: 'output' });
        this.addComponent('energy_bar', 161, 16);
        break;
      case 'dual_input':
        this.addComponent('slot', 45, 25, { slotType: 'input' });
        this.addComponent('slot', 45, 47, { slotType: 'input' });
        this.addComponent('progress_arrow', 73, 35);
        this.addComponent('big_slot', 107, 30, { slotType: 'output' });
        this.addComponent('energy_bar', 161, 16);
        break;
      case 'single_slot':
        this.addComponent('slot', 79, 34, { slotType: 'input' });
        this.addComponent('energy_bar', 161, 16);
        break;
      case 'tank':
        this.addComponent('fluid_tank', 80, 16);
        this.addComponent('energy_bar', 161, 16);
        break;
    }
  }

  serialize() {
    return this.components.map(c => ({ ...c }));
  }

  deserialize(data) {
    this.components = (data || []).map(c => ({ ...c }));
    this.selectedIndex = -1;
    this.render();
  }

  onMouseDown(e) {
    const p = this.toGuiCoords(e);
    // Find component under cursor (reverse order for top-most)
    for (let i = this.components.length - 1; i >= 0; i--) {
      const c = this.components[i];
      if (p.x >= c.x && p.x < c.x + c.w && p.y >= c.y && p.y < c.y + c.h) {
        this.selectedIndex = i;
        this.dragging = true;
        this.dragOffset = { x: p.x - c.x, y: p.y - c.y };
        this.render();
        return;
      }
    }
    this.selectedIndex = -1;
    this.render();
  }

  onMouseMove(e) {
    if (!this.dragging || this.selectedIndex < 0) return;
    const p = this.toGuiCoords(e);
    const c = this.components[this.selectedIndex];
    c.x = this.snap(Math.max(3, Math.min(GUI_W - c.w - 3, p.x - this.dragOffset.x)));
    c.y = this.snap(Math.max(3, Math.min(GUI_H - c.h - 3, p.y - this.dragOffset.y)));
    this.render();
  }

  onMouseUp() {
    this.dragging = false;
  }

  onRightClick(e) {
    const p = this.toGuiCoords(e);
    for (let i = this.components.length - 1; i >= 0; i--) {
      const c = this.components[i];
      if (p.x >= c.x && p.x < c.x + c.w && p.y >= c.y && p.y < c.y + c.h) {
        this.removeComponent(i);
        return;
      }
    }
  }

  render() {
    const ctx = this.ctx;
    ctx.imageSmoothingEnabled = false;

    // Clear
    ctx.fillStyle = '#1a1a1e';
    ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

    ctx.save();
    ctx.scale(SCALE, SCALE);

    // Panel background
    const [bgR, bgG, bgB] = COLORS.BG;
    ctx.fillStyle = `rgb(${bgR},${bgG},${bgB})`;
    ctx.fillRect(0, 0, GUI_W, GUI_H);

    // Panel border (3D bevel)
    ctx.fillStyle = `rgb(${COLORS.WH.join(',')})`;
    ctx.fillRect(0, 0, GUI_W, 1);
    ctx.fillRect(0, 0, 1, GUI_H);
    ctx.fillStyle = `rgb(${COLORS.DK.join(',')})`;
    ctx.fillRect(1, GUI_H - 1, GUI_W - 1, 1);
    ctx.fillRect(GUI_W - 1, 1, 1, GUI_H - 1);
    ctx.fillStyle = `rgb(${COLORS.BK.join(',')})`;
    ctx.fillRect(0, GUI_H - 1, 1, 1);
    ctx.fillRect(GUI_W - 1, 0, 1, 1);

    // Player inventory area (always at bottom)
    this.drawPlayerInventory(ctx, 7, 83);

    // Draw components
    for (let i = 0; i < this.components.length; i++) {
      const c = this.components[i];
      this.drawComponent(ctx, c, i === this.selectedIndex);
    }

    ctx.restore();
  }

  drawComponent(ctx, comp, selected) {
    const { type, x, y, w, h } = comp;
    const def = COMP_DEFS[type];

    switch (type) {
      case 'slot':
      case 'big_slot':
        this.drawSlot(ctx, x, y, w, h);
        break;
      case 'energy_bar':
        this.drawSlotBorder(ctx, x, y, w, h);
        ctx.fillStyle = `rgb(${COLORS.BAR_BG.join(',')})`;
        ctx.fillRect(x + 1, y + 1, w - 2, h - 2);
        break;
      case 'progress_arrow':
        ctx.fillStyle = '#686868';
        ctx.fillRect(x, y, w, h);
        // Arrow shape
        ctx.fillStyle = `rgb(${COLORS.BG.join(',')})`;
        for (let dy = 0; dy < 7; dy++) {
          ctx.fillRect(x + w - 7 + dy, y + dy, 1, 1);
          ctx.fillRect(x + w - 7 + dy, y + h - 1 - dy, 1, 1);
        }
        break;
      case 'flame':
        ctx.fillStyle = '#686868';
        ctx.fillRect(x, y, w, h);
        break;
      case 'fluid_tank':
        this.drawSlotBorder(ctx, x, y, w, h);
        ctx.fillStyle = '#1a3a6b';
        ctx.fillRect(x + 1, y + 1, w - 2, h - 2);
        break;
      case 'gas_tank':
        this.drawSlotBorder(ctx, x, y, w, h);
        ctx.fillStyle = '#3a3a3a';
        ctx.fillRect(x + 1, y + 1, w - 2, h - 2);
        break;
      case 'separator':
        ctx.fillStyle = `rgb(${COLORS.DK.join(',')})`;
        ctx.fillRect(x, y, w, 1);
        ctx.fillStyle = `rgb(${COLORS.WH.join(',')})`;
        ctx.fillRect(x, y + 1, w, 1);
        break;
    }

    if (selected) {
      ctx.strokeStyle = '#3b82f6';
      ctx.lineWidth = 1 / SCALE;
      ctx.strokeRect(x - 1, y - 1, w + 2, h + 2);
    }
  }

  drawSlot(ctx, x, y, w, h) {
    this.drawSlotBorder(ctx, x, y, w, h);
    ctx.fillStyle = `rgb(${COLORS.SL.join(',')})`;
    ctx.fillRect(x + 1, y + 1, w - 2, h - 2);
  }

  drawSlotBorder(ctx, x, y, w, h) {
    ctx.fillStyle = `rgb(${COLORS.SD.join(',')})`;
    ctx.fillRect(x, y, w, 1);
    ctx.fillRect(x, y, 1, h);
    ctx.fillStyle = `rgb(${COLORS.WH.join(',')})`;
    ctx.fillRect(x + 1, y + h - 1, w - 1, 1);
    ctx.fillRect(x + w - 1, y + 1, 1, h - 1);
  }

  drawPlayerInventory(ctx, sx, sy) {
    // 3 rows of 9
    for (let r = 0; r < 3; r++) {
      for (let c = 0; c < 9; c++) {
        this.drawSlot(ctx, sx + c * 18, sy + r * 18, 18, 18);
      }
    }
    // Hotbar (gap of 4px)
    for (let c = 0; c < 9; c++) {
      this.drawSlot(ctx, sx + c * 18, sy + 58, 18, 18);
    }
  }
}

export { COMP_DEFS };
