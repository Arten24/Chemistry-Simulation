import { clamp } from './rng.js'; export function fadeAt(lt){ return clamp(Math.min(lt/0.045,(1-lt)/0.045),0,1); }
