const fs = require('fs');
const d = require('./megacrusher_elements.json');
let o = 'public static final float[][] MEGA_CRUSHER_PARTS = {\n';
d.forEach(e => {
  o += '  {' + e.from.map(x=>x+'F').join(', ') + ', ' + e.to.map(x=>x+'F').join(', ') + '}, // ' + e.name + '\n';
});
o += '};\n';
fs.writeFileSync('megacrusher_parts.java', o);
