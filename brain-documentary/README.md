# 10 Mind-Blowing Psychology Facts About Your Brain — Cinematic Documentary Generator
A complete, automated system that renders a **600-second (10:00), 1920×1080, 30 fps (18,000-frame)** cinematic
neuroscience documentary with Three.js/WebGL, a procedurally generated cortical-fold 3D brain, glowing neural
networks, GPU particle/pulse shaders, bloom, real AI narration, procedural ambient music + SFX, and synchronised subtitles.

## Quick start
```bash
npm install
npm run preview     # http://localhost:5173 — play, scrub the 10-minute timeline, jump between 12 chapters
npm run render      # -> output/final_brain_psychology_documentary.mp4  (1920x1080, 30fps, 18000 frames)
npm run render:fast # 1280x720, 24fps
npm run render:high # 1920x1080, 60fps
npm run render:sample  # 5-second proof render -> output/sample.mp4
npm run check       # validate assets + syntax + 600s timeline
npm run assets      # verify procedural assets (nothing to download)
npm run audio       # regenerate music/SFX + verify narration
```
Requires: Node.js 20+, npm. FFmpeg is bundled via `ffmpeg-static` (no separate install needed). Works on Windows 10/11,
macOS and Linux; all scripts are Node (no Bash-only commands). Rendering is deterministic: `renderFrame(t)` is a pure
function of the timestamp, so frame N always looks identical. Full renders take a long time (hours on modest hardware —
18,000 WebGL frames); use `render:fast` or `render:sample` first.

## Chapters (12)
00:00 Intro · 00:30 Neuroplasticity · 01:25 Predictive perception · 02:20 Reconstructed memory · 03:15 Brain energy (20%) ·
04:10 Sleep & memory · 05:05 Emotion & decisions · 06:00 Limited attention · 06:55 Automatic habits ·
07:50 Stress & memory (balanced, evidence-based) · 08:45 Brain networks · 09:40 Conclusion.

## Structure
See the folder tree in the brief: `src/` (brain, neurons, particles, camera, lighting, materials, timeline, typography,
transitions, 12 chapter modules, GLSL shaders), `scripts/` (render, generate-audio, generate-subtitles,
validate-assets, download-assets, check-syntax), `assets/` (voiceover MP3s, generated music/SFX, models note),
`subtitles/subtitles.srt`, `output/`.

## Honest limitations
- The brain is a high-detail *procedural* cortical-fold mesh — not a scanned anatomical GLB (see ASSET_LICENSES.md).
- Narration is an AI voice generated for this project; chapter files are 10 segments covering all 12 chapters
  (seg01 = intro + fact 1, seg10 = fact 10 + conclusion), each placed at its exact chapter timestamp by the mixer.
- Captions are rendered into every frame (always visible) and the SRT is also embedded as a mov_text subtitle track.
- A full 1080p30 render is 18,000 frames; expect hours, not minutes, on a typical laptop.
