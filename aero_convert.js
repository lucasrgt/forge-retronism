const fs = require('fs');
const path = require('path');

const inputPath = process.argv[2];

if (!inputPath) {
    console.log("Uso: node aero_convert.js <seu_modelo.bbmodel>");
    process.exit(1);
}

const modelName = path.basename(inputPath, path.extname(inputPath));
const bbData = JSON.parse(fs.readFileSync(inputPath, 'utf8'));

// 1. Extrair Pivots e Animações
const animBundle = {
    format_version: "1.0",
    pivots: {},
    animations: {}
};

// Recursivamente encontra todos os grupos e seus pivots
function processOutliner(items) {
    if (!items) return;
    items.forEach(item => {
        // Formato Generic (item.type === 'group')
        if (item.type === 'group') {
            if (item.name && item.origin) {
                animBundle.pivots[item.name] = [item.origin[0], item.origin[1], item.origin[2]];
            }
            if (item.children) processOutliner(item.children);
        } 
        // Formato Bedrock (item é um UUID referenciando o array groups)
        else if (typeof item === 'string' && bbData.groups) {
            const group = bbData.groups.find(g => g.uuid === item);
            if (group) {
                animBundle.pivots[group.name] = [group.origin[0], group.origin[1], group.origin[2]];
                if (group.children) processOutliner(group.children);
            }
        }
        else if (item.uuid && bbData.groups) {
             const group = bbData.groups.find(g => g.uuid === item.uuid);
             if (group) {
                 animBundle.pivots[group.name] = [group.origin[0], group.origin[1], group.origin[2]];
                 if (item.children) processOutliner(item.children);
             }
        }
    });
}

// Também processar o array 'groups' diretamente caso o outliner esteja vazio ou em formato diferente
if (bbData.groups) {
    bbData.groups.forEach(group => {
        if (group.name && group.origin) {
            animBundle.pivots[group.name] = [group.origin[0], group.origin[1], group.origin[2]];
        }
    });
}

if (bbData.outliner) {
    processOutliner(bbData.outliner);
}

// Mapeia as animações e seus keyframes
if (bbData.animations) {
    bbData.animations.forEach(anim => {
        // Normaliza o nome da animação (ex: spin -> animation.spin ou working -> animation.working)
        let aName = anim.name;
        if (!aName.startsWith("animation.")) {
            // Se o Java esperar 'spin', mas no BB for 'working', fazemos o mapeamento manual aqui se necessário
            // Por enquanto, apenas garantimos o prefixo que a engine costuma usar
            if (aName === "working") aName = "spin"; // Mapeamento específico para o MegaCrusher
        }
        
        const animation = {
            loop: anim.loop === 'loop' || anim.loop === true,
            length: anim.length,
            bones: {}
        };

        for (let boneId in anim.animators) {
            const animator = anim.animators[boneId];
            const boneName = animator.name; // Nome do grupo no Blockbench
            const boneData = {};

            if (animator.keyframes && animator.keyframes.length > 0) {
                animator.keyframes.forEach(kf => {
                    if (!boneData[kf.channel]) boneData[kf.channel] = {};
                    
                    try {
                        const dataPoints = typeof kf.data_points === 'string' ? JSON.parse(kf.data_points) : kf.data_points;
                        const val = dataPoints[0];
                        
                        // Captura X, Y, Z do keyframe
                        let vx = parseFloat(val.x) || 0;
                        let vy = parseFloat(val.y) || 0;
                        let vz = parseFloat(val.z) || 0;
                        
                        boneData[kf.channel][kf.time] = [vx, vy, vz];
                    } catch (e) {
                        console.error(`Erro ao processar keyframe em ${boneName}:`, e);
                    }
                });
            }
            
            if (Object.keys(boneData).length > 0) {
                animation.bones[boneName] = boneData;
            }
        }
        animBundle.animations[aName] = animation;
    });
}

// 2. Salvar o arquivo de animação
const assetsDir = path.join('src', 'retronism', 'assets', 'models');
if (!fs.existsSync(assetsDir)) fs.mkdirSync(assetsDir, { recursive: true });

const outPath = path.join(assetsDir, `${modelName}.anim.json`);
fs.writeFileSync(outPath, JSON.stringify(animBundle, null, 2));

console.log(`\n🚀 Aero Workflow: Sincronização Concluída!`);
console.log(`----------------------------------------`);
console.log(`✅ Modelo lido: ${inputPath}`);
console.log(`✅ Grupos exportados: ${Object.keys(animBundle.pivots).length}`);
console.log(`✅ Animações geradas: ${Object.keys(animBundle.animations).join(', ')}`);
console.log(`✅ Destino: ${outPath}`);
console.log(`----------------------------------------`);
console.log(`Agora exporte o OBJ no Blockbench e rode o jogo!`);
