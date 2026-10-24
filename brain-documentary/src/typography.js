export function showChapter(el, chapter, lt){
  el.kicker.textContent=chapter.kicker; el.title.textContent=chapter.title;
  const inn = Math.min(1, lt/0.08), out = Math.min(1,(1-lt)/0.08);
  el.box.style.opacity = String(Math.max(0,Math.min(inn,out)*0.98));
  el.box.style.transform = `translateY(${(1-Math.min(inn,1))*24}px)`;
}
export function captionFor(sentences, t){
  // sentences have estimated start/end assigned in main from narration timing model
  return sentences.find(s=>t>=s.start && t<s.end) || null;
}
