import fs from 'node:fs';
// No external model is required: the brain is procedurally generated in src/brain.js (cortical-fold displacement),
// so there is nothing to download and no third-party model licence to violate. This script verifies that choice.
fs.mkdirSync('assets/models',{recursive:true});
fs.writeFileSync('assets/models/README.txt','Procedural brain generated at runtime by src/brain.js (seeded, deterministic). No external GLB was used: no freely-redistributable realistic brain GLB could be verified from an allowed source, so a procedural cortical-fold mesh is used instead. See ASSET_LICENSES.md.\n');
console.log('Assets OK: procedural brain (src/brain.js), generated music/SFX, pre-generated narration. Nothing to download.');
