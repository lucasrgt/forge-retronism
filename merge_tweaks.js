const fs = require('fs');
const d = JSON.parse(fs.readFileSync('./megacrusher_elements.json', 'utf8'));

// Helper to check for the custom tweaks I applied in Blockbench session
function getTweak(name) {
    if (name === 'base_platform') return { dy: -0.01, inf: 0 };
    if (name.includes('bracket')) return { dy: 0, inf: 0.01 };
    if (name.includes('pillar')) return { dy: 0, inf: 0.005 };
    if (name.includes('rail')) return { dy: 0, inf: 0.002 };
    return { dy: 0, inf: 0 };
}

let o = '    public static final float[][] MEGA_CRUSHER_PARTS_UVS = {\n';
let render_parts = '    public static final float[][] MEGA_CRUSHER_PARTS = {\n';

const faces = ['down', 'up', 'north', 'south', 'west', 'east'];

d.forEach(e => {
  let tweak = getTweak(e.name);
  let f = [e.from[0] - tweak.inf, e.from[1] - tweak.inf + tweak.dy, e.from[2] - tweak.inf];
  let t = [e.to[0] + tweak.inf, e.to[1] + tweak.inf + tweak.dy, e.to[2] + tweak.inf];
  
  const fmt = n => Number(n.toFixed(4)) + 'F';
  
  let line = `      {${fmt(f[0])}, ${fmt(f[1])}, ${fmt(f[2])}, ${fmt(t[0])}, ${fmt(t[1])}, ${fmt(t[2])}, `;
  
  faces.forEach(faceName => {
    if (e.faces && e.faces[faceName]) {
      let uv = e.faces[faceName].uv;
      line += `${uv[0]}F, ${uv[1]}F, ${uv[2]}F, ${uv[3]}F, `;
    } else {
      line += `-1F, -1F, -1F, -1F, `;
    }
  });
  
  line += `}, // ${e.name}\n`;
  o += line;
  
  render_parts += `      {${fmt(f[0])}, ${fmt(f[1])}, ${fmt(f[2])}, ${fmt(t[0])}, ${fmt(t[1])}, ${fmt(t[2])}}, // ${e.name}\n`;
});

o += '    };\n';
render_parts += '    };\n';

fs.writeFileSync('megacrusher_parts_uvs.java', o);
fs.writeFileSync('megacrusher_parts_standard.java', render_parts);
console.log("Done generating corrected arrays.");
