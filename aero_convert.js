const fs = require('fs');
const path = require('path');

const inputPath = process.argv[2];
const className = process.argv[3];

if (!inputPath || !className) {
    console.log("Uso: node aero_convert.js <arquivo.json> <NomeDaClasse>");
    process.exit(1);
}

const data = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
const elements = data.elements || [];
const textureWidth = data.resolution ? data.resolution.width : 128;

// Tweak Settings
const tweaks = {
    'pillar': { inflate: 0.005 },
    'rail': { inflate: 0.002 },
    'bracket': { inflate: 0.01, dy: -0.01 },
    'base_platform': { dy: -0.01 }
};

let javaOutput = `package retronism.render;\n\nimport retronism.aero.Aero_Model;\n\n`;
javaOutput += `public class ${className} {\n\n`;
javaOutput += `    public static final Aero_Model MODEL = new Aero_Model("${className}", new float[][] {\n`;

elements.forEach(el => {
    let { from, to, faces, name } = el;
    let inf = 0;
    let dy = 0;
    
    // Auto-tweak by name
    for (const key in tweaks) {
        if (name.includes(key)) {
            if (tweaks[key].inflate) inf = tweaks[key].inflate;
            if (tweaks[key].dy) dy = tweaks[key].dy;
        }
    }

    const f = [
        from[0] - inf, from[1] - inf + dy, from[2] - inf,
        to[0] + inf, to[1] + inf + dy, to[2] + inf
    ];

    let line = `      {${f.map(n => n.toFixed(3) + 'F').join(', ')}, `;
    
    const faceOrder = ['down', 'up', 'north', 'south', 'west', 'east'];
    faceOrder.forEach(faceName => {
        const face = faces[faceName];
        if (face && face.uv) {
            line += `${face.uv.map(n => n.toFixed(1) + 'F').join(', ')}, `;
        } else {
            line += `-1F, -1F, -1F, -1F, `;
        }
    });

    javaOutput += `${line}}, // ${name}\n`;
});

javaOutput += `    }, ${textureWidth}.0f, 16.0f);\n\n}`;

const outPath = `src/retronism/render/${className}.java`.replace(/\//g, path.sep);
fs.writeFileSync(outPath, javaOutput);
console.log(`Sucesso! Modelo Aero convertido para: ${outPath}`);
