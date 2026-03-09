const fs = require('fs');
const data = JSON.parse(fs.readFileSync('parts.json', 'utf8'));

let output = '    public static final float[][] MEGA_CRUSHER_PARTS = {\n';

data.forEach(e => {
    let f = e.from;
    let t = e.to;
    let inf = e.inflate || 0;
    
    // Apply inflate to the coordinates
    let fX = f[0] - inf;
    let fY = f[1] - inf;
    let fZ = f[2] - inf;
    let tX = t[0] + inf;
    let tY = t[1] + inf;
    let tZ = t[2] + inf;
    
    // Formatting numbers to avoid too many decimals
    const fmt = n => Number(n.toFixed(4)) + 'F';
    
    output += `      {${fmt(fX)}, ${fmt(fY)}, ${fmt(fZ)}, ${fmt(tX)}, ${fmt(tY)}, ${fmt(tZ)}}, // ${e.name}\n`;
});

output += '    };\n';
console.log(output);
fs.writeFileSync('megacrusher_parts.txt', output);
