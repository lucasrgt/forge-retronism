/**
 * aero_strip.js — Converte .bbmodel Blockbench → JSON leve para Aero_ModelLoader
 *
 * Remove texturas embedadas (base64), animações e metadados desnecessários.
 * O JSON resultante tem apenas elements + resolution — pronto para o jar.
 *
 * Uso: node aero_strip.js <arquivo.bbmodel> [saida.aero.json]
 * Padrão de saída: src/retronism/assets/models/<nome>.aero.json
 */

const fs   = require('fs');
const path = require('path');

const inputPath = process.argv[2];
if (!inputPath) {
    console.log("Uso: node aero_strip.js <arquivo.bbmodel> [saida.aero.json]");
    process.exit(1);
}

const data = JSON.parse(fs.readFileSync(inputPath, 'utf8'));

// Extrair apenas elements com from+to (pula meshes, locators, etc)
const elements = (data.elements || [])
    .filter(el => el.from && el.to)
    .map(el => {
        const faces = {};
        for (const face of ['down', 'up', 'north', 'south', 'west', 'east']) {
            if (el.faces && el.faces[face] && el.faces[face].uv) {
                faces[face] = { uv: el.faces[face].uv };
            }
        }
        const out = { name: el.name, from: el.from, to: el.to, faces };
        // Preservar inflate se existir (afeta bounding box)
        if (el.inflate) out.inflate = el.inflate;
        return out;
    });

// Resolution para textureSize
const resolution = data.resolution || { width: 128, height: 128 };

const output = { resolution, elements };

// Determinar caminho de saída
const baseName = path.basename(inputPath, path.extname(inputPath));
const outPath = process.argv[3] ||
    path.join('src', 'retronism', 'assets', 'models', baseName + '.aero.json');

fs.writeFileSync(outPath, JSON.stringify(output));

console.log(`Stripped: ${elements.length} elementos → ${outPath}`);
console.log(`Tamanho: ${(fs.statSync(inputPath).size / 1024).toFixed(1)}KB → ${(fs.statSync(outPath).size / 1024).toFixed(1)}KB`);
