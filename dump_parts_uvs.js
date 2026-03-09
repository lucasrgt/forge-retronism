const fs = require('fs');
const d = JSON.parse(fs.readFileSync('./megacrusher_elements.json', 'utf8'));

let o = '    public static final float[][] MEGA_CRUSHER_PARTS_UVS = {\n';

const faces = ['down', 'up', 'north', 'south', 'west', 'east'];

d.forEach(e => {
  let line = `      {${e.from[0]}F, ${e.from[1]}F, ${e.from[2]}F, ${e.to[0]}F, ${e.to[1]}F, ${e.to[2]}F, `;
  
  faces.forEach(f => {
    if (e.faces && e.faces[f]) {
      let uv = e.faces[f].uv;
      line += `${uv[0]}F, ${uv[1]}F, ${uv[2]}F, ${uv[3]}F, `;
    } else {
      line += `-1F, -1F, -1F, -1F, `;
    }
  });
  
  line += `}, // ${e.name}\n`;
  o += line;
});

o += '    };\n';
fs.writeFileSync('megacrusher_parts_uvs.java', o);
console.log("Done");
