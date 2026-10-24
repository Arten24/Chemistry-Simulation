export const DURATION=600;
 export const CHAPTERS=[
 {
  "id": "intro",
  "kicker": "INTRODUCTION",
  "title": "The Most Complex Object in the Known Universe",
  "start": 0,
  "end": 30,
  "mode": "orbit"
 },
 {
  "id": "neuroplasticity",
  "kicker": "FACT 1",
  "title": "Your Brain Can Change Itself \u2014 Neuroplasticity",
  "start": 30,
  "end": 85,
  "mode": "plasticity"
 },
 {
  "id": "perception",
  "kicker": "FACT 2",
  "title": "Your Brain Predicts Reality",
  "start": 85,
  "end": 140,
  "mode": "eye"
 },
 {
  "id": "memory",
  "kicker": "FACT 3",
  "title": "Memories Are Reconstructed",
  "start": 140,
  "end": 195,
  "mode": "hippocampus"
 },
 {
  "id": "energy",
  "kicker": "FACT 4",
  "title": "Your Brain Uses About 20% of Your Energy",
  "start": 195,
  "end": 250,
  "mode": "energy"
 },
 {
  "id": "sleep",
  "kicker": "FACT 5",
  "title": "Sleep Helps Consolidate Memories",
  "start": 250,
  "end": 305,
  "mode": "sleep"
 },
 {
  "id": "emotions",
  "kicker": "FACT 6",
  "title": "Emotions Influence Decisions",
  "start": 305,
  "end": 360,
  "mode": "amygdala"
 },
 {
  "id": "attention",
  "kicker": "FACT 7",
  "title": "Attention Is Limited",
  "start": 360,
  "end": 415,
  "mode": "attention"
 },
 {
  "id": "habits",
  "kicker": "FACT 8",
  "title": "Habits Become Automatic With Practice",
  "start": 415,
  "end": 470,
  "mode": "habit"
 },
 {
  "id": "stress",
  "kicker": "FACT 9",
  "title": "Stress Can Affect Memory",
  "start": 470,
  "end": 525,
  "mode": "stress"
 },
 {
  "id": "networks",
  "kicker": "FACT 10",
  "title": "Your Brain Works Through Networks",
  "start": 525,
  "end": 580,
  "mode": "network"
 },
 {
  "id": "outro",
  "kicker": "CONCLUSION",
  "title": "The Universe Between Your Ears",
  "start": 580,
  "end": 600,
  "mode": "pullback"
 }
];
export function chapterAt(t){return CHAPTERS.find(c=>t>=c.start&&t<c.end)||CHAPTERS[CHAPTERS.length-1];}
export function localT(t){const c=chapterAt(t);return (t-c.start)/(c.end-c.start);}
