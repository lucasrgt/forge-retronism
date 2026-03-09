const data = JSON.parse(require('fs').readFileSync('src/retronism/assets/models/MegaCrusher.bbmodel', 'utf8'));
console.log('resolution:', data.resolution);
console.log('textures count:', (data.textures||[]).length);
if (data.textures && data.textures[0]) {
  const t = data.textures[0];
  console.log('textures[0].width:', t.width, 'height:', t.height, 'name:', t.name);
}
const faces = ['down','up','north','south','west','east'];
const els = (data.elements || []).filter(e => e.from && e.to);
console.log('Total cube elements:', els.length);

// Check element-level rotations
const rotated = els.filter(e => e.rotation && e.rotation.angle && e.rotation.angle !== 0);
console.log('Elements with 3D rotation:', rotated.length);
rotated.slice(0, 5).forEach(e => console.log(' ', e.name, JSON.stringify(e.rotation)));

// Check for zero UV and out-of-range UV
let badUV = 0, oobUV = 0;
const ts = (data.resolution || {}).width || 128;
els.forEach(el => {
  faces.forEach(face => {
    const f = (el.faces || {})[face];
    if (!f || !f.uv) return;
    const uv = f.uv;
    if (uv[0] === 0 && uv[1] === 0 && uv[2] === 0 && uv[3] === 0) {
      badUV++;
      console.log('  zero UV:', el.name, face);
    }
    if (uv[0] < 0 || uv[1] < 0 || uv[2] > ts || uv[3] > ts) {
      oobUV++;
      console.log('  OOB UV:', el.name, face, uv);
    }
  });
});
console.log('Faces with [0,0,0,0] UV:', badUV);
console.log('Faces with out-of-range UV:', oobUV);

// Show all element names to spot anything unusual
console.log('\nAll elements:');
els.forEach(el => console.log(' ', el.name));
