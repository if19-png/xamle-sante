// Objets de santé décoratifs qui flottent dans les marges du site (aucune interaction, masqués aux lecteurs d'écran).
const V = '#0b7a5b', T = '#14b8a6', R = '#d32f2f';

const Croix = () => (
  <svg viewBox="0 0 64 64"><path d="M24 8h16v16h16v16H40v16H24V40H8V24h16z" fill={T} stroke={V} strokeWidth="4" strokeLinejoin="round" /></svg>
);
const Pilule = () => (
  <svg viewBox="0 0 64 64"><g transform="rotate(-35 32 32)">
    <rect x="6" y="21" width="52" height="22" rx="11" fill="#fff" stroke={V} strokeWidth="3" />
    <path d="M17 21h15v22H17a11 11 0 0 1 0-22z" fill={R} stroke={V} strokeWidth="3" strokeLinejoin="round" />
  </g></svg>
);
const Coeur = () => (
  <svg viewBox="0 0 64 64"><path d="M32 56C10 40 6 26 6 19a13 13 0 0 1 26-3 13 13 0 0 1 26 3c0 7-4 21-26 37z" fill={R} />
    <polyline points="14,33 24,33 28,24 34,42 38,30 50,30" fill="none" stroke="#fff" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" /></svg>
);
const Goutte = () => (
  <svg viewBox="0 0 64 64"><path d="M32 6C32 6 12 28 12 40a20 20 0 0 0 40 0C52 28 32 6 32 6z" fill={T} />
    <path d="M22 40a10 10 0 0 0 7 9" fill="none" stroke="#fff" strokeWidth="3" strokeLinecap="round" opacity=".8" /></svg>
);
const Stetho = () => (
  <svg viewBox="0 0 64 64" fill="none" stroke={V} strokeWidth="4" strokeLinecap="round">
    <path d="M18 8v16a14 14 0 0 0 28 0V8" /><path d="M32 38v6a10 10 0 0 0 20 0v-4" />
    <circle cx="18" cy="8" r="3" fill={V} /><circle cx="46" cy="8" r="3" fill={V} /><circle cx="52" cy="35" r="5" fill={T} /></svg>
);
const Thermo = () => (
  <svg viewBox="0 0 64 64"><rect x="26" y="6" width="12" height="38" rx="6" fill="#fff" stroke={V} strokeWidth="3" />
    <circle cx="32" cy="49" r="10" fill={R} stroke={V} strokeWidth="3" /><rect x="30" y="20" width="4" height="26" rx="2" fill={R} />
    <path d="M42 14h6M42 22h4M42 30h6" stroke={V} strokeWidth="3" strokeLinecap="round" /></svg>
);
const Adn = () => (
  <svg viewBox="0 0 64 64" fill="none" strokeWidth="4" strokeLinecap="round">
    <path d="M20 6C44 20 44 44 20 58" stroke={V} /><path d="M44 6C20 20 20 44 44 58" stroke={T} />
    <path d="M27 17h10M22 32h20M27 47h10" stroke={V} strokeWidth="3" /></svg>
);
const Bouclier = () => (
  <svg viewBox="0 0 64 64"><path d="M32 6l22 8v18c0 14-10 22-22 26C20 54 10 46 10 32V14z" fill={V} />
    <path d="M28 22h8v7h7v8h-7v7h-8v-7h-7v-8h7z" fill="#fff" /></svg>
);

// côté, position verticale (%), horizontale dans la marge (%), taille (px max), durée et décalage d'animation (s), inclinaison (deg)
const MARGES = [
  { Obj: Stetho,   cote: 'g', top: 12, x: 40, t: 84, d: 9,  r: 8,   rot: -10 },
  { Obj: Pilule,   cote: 'g', top: 36, x: 62, t: 74, d: 11, r: 3,   rot: 12 },
  { Obj: Coeur,    cote: 'g', top: 60, x: 34, t: 80, d: 8,  r: 5.5, rot: -6 },
  { Obj: Adn,      cote: 'g', top: 82, x: 58, t: 70, d: 12, r: 1,   rot: 14 },
  { Obj: Croix,    cote: 'd', top: 16, x: 55, t: 70, d: 10, r: 2,   rot: 8 },
  { Obj: Goutte,   cote: 'd', top: 38, x: 34, t: 66, d: 8,  r: 6.5, rot: -8 },
  { Obj: Thermo,   cote: 'd', top: 62, x: 60, t: 84, d: 11, r: 4,   rot: 10 },
  { Obj: Bouclier, cote: 'd', top: 84, x: 38, t: 74, d: 9,  r: 7.5, rot: -12 },
];

const HERO = [
  { Obj: Coeur,  top: 12, x: 62, t: 96, d: 8,  r: 1,   rot: -8 },
  { Obj: Croix,  top: 56, x: 80, t: 72, d: 10, r: 4,   rot: 10 },
  { Obj: Pilule, top: 48, x: 46, t: 84, d: 12, r: 6.5, rot: 14 },
];

function Objet({ Obj, top, x, t, d, r, rot }) {
  return (
    <span className="flot" style={{ top: `${top}%`, left: `${x}%`, '--t': `${t}px`, '--d': `${d}s`, '--r': `-${r}s`, '--rot': `${rot}deg` }}>
      <Obj />
    </span>
  );
}

export default function Flottants({ variante }) {
  if (variante === 'hero') {
    return <div className="flot-hero" aria-hidden="true">{HERO.map((o, i) => <Objet key={i} {...o} />)}</div>;
  }
  return (
    <div aria-hidden="true">
      <div className="flot-marge flot-g">{MARGES.filter((o) => o.cote === 'g').map((o, i) => <Objet key={i} {...o} />)}</div>
      <div className="flot-marge flot-d">{MARGES.filter((o) => o.cote === 'd').map((o, i) => <Objet key={i} {...o} />)}</div>
    </div>
  );
}
