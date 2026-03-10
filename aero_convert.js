const fs = require('fs');
const path = require('path');

const inputPath = process.argv[2];

if (!inputPath) {
    console.log("Usage: node aero_convert.js <your_model.bbmodel>");
    process.exit(1);
}

const modelName = path.basename(inputPath, path.extname(inputPath));
const bbData = JSON.parse(fs.readFileSync(inputPath, 'utf8'));

// 1. Extract Pivots and Animations
const animBundle = {
    format_version: "1.0",
    pivots: {},
    childMap: {},
    animations: {}
};

// Recursively finds all groups and their pivots
function processOutliner(items) {
    if (!items) return;
    items.forEach(item => {
        // Generic format (item.type === 'group')
        if (item.type === 'group') {
            if (item.name && item.origin) {
                animBundle.pivots[item.name] = [item.origin[0], item.origin[1], item.origin[2]];
            }
            if (item.children) processOutliner(item.children);
        } 
        // Bedrock format (item is a UUID referencing the groups array)
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

// Also process the 'groups' array directly in case the outliner is empty or in a different format
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

// 1b. Extract childMap: maps each child element/group name → parent animated group name
// This allows the renderer to know that "shred_blade_L_0_0" belongs to bone "shredder_L"
const elementNameMap = {};
if (bbData.elements) {
    bbData.elements.forEach(el => { if (el.uuid && el.name) elementNameMap[el.uuid] = el.name; });
}
// Groups can also be children (sub-groups)
if (bbData.groups) {
    bbData.groups.forEach(g => { if (g.uuid && g.name) elementNameMap[g.uuid] = g.name; });
}

function buildChildMap(items, parentGroupName) {
    if (!items) return;
    items.forEach(item => {
        if (typeof item === 'string') {
            // UUID of element or group
            const childName = elementNameMap[item];
            if (childName && parentGroupName) {
                animBundle.childMap[childName] = parentGroupName;
            }
        } else if (typeof item === 'object') {
            // Sub-group in the outliner
            const groupName = item.name || (bbData.groups && bbData.groups.find(g => g.uuid === item.uuid) || {}).name;
            if (groupName && parentGroupName) {
                animBundle.childMap[groupName] = parentGroupName;
            }
            if (item.children) {
                buildChildMap(item.children, groupName || parentGroupName);
            }
        }
    });
}
if (bbData.outliner) {
    buildChildMap(bbData.outliner, null);
}

// Maps animations and their keyframes
if (bbData.animations) {
    bbData.animations.forEach(anim => {
        // Normalize animation name (e.g. spin -> animation.spin or working -> animation.working)
        let aName = anim.name;
        // Remove "animation." prefix if present
        if (aName.startsWith("animation.")) {
            aName = aName.substring("animation.".length);
        }
        const animation = {
            loop: anim.loop === 'loop' || anim.loop === true,
            length: anim.length,
            bones: {}
        };

        for (let boneId in anim.animators) {
            const animator = anim.animators[boneId];
            const boneName = animator.name; // Group name in Blockbench
            const boneData = {};

            if (animator.keyframes && animator.keyframes.length > 0) {
                animator.keyframes.forEach(kf => {
                    if (!boneData[kf.channel]) boneData[kf.channel] = {};
                    
                    try {
                        const dataPoints = typeof kf.data_points === 'string' ? JSON.parse(kf.data_points) : kf.data_points;
                        const val = dataPoints[0];
                        
                        // Capture X, Y, Z from keyframe
                        let vx = parseFloat(val.x) || 0;
                        let vy = parseFloat(val.y) || 0;
                        let vz = parseFloat(val.z) || 0;
                        
                        boneData[kf.channel][kf.time] = [vx, vy, vz];
                    } catch (e) {
                        console.error(`Error processing keyframe in ${boneName}:`, e);
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

// 2. Save the animation file
const assetsDir = path.join('src', 'retronism', 'assets', 'models');
if (!fs.existsSync(assetsDir)) fs.mkdirSync(assetsDir, { recursive: true });

const outPath = path.join(assetsDir, `${modelName}.anim.json`);
fs.writeFileSync(outPath, JSON.stringify(animBundle, null, 2));

console.log(`\nAero Workflow: Sync Complete!`);
console.log(`----------------------------------------`);
console.log(`Model read: ${inputPath}`);
console.log(`Groups exported: ${Object.keys(animBundle.pivots).length}`);
console.log(`Animations generated: ${Object.keys(animBundle.animations).join(', ')}`);
console.log(`Output: ${outPath}`);
console.log(`----------------------------------------`);
console.log(`Now export the OBJ in Blockbench and run the game!`);
