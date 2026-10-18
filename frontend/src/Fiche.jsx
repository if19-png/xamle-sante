import { useEffect, useState } from 'react';
import api, { msgErreur } from './api';
import { useAuth } from './auth';

export const JOURS = ['', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'];

/** Photo ronde du spécialiste, ou ses initiales si le lien est absent ou cassé. */
export function Avatar({ prenom, nom, url, taille = 56 }) {
  const [casse, setCasse] = useState(false);
  const ini = `${(prenom || '?').charAt(0)}${(nom || '').charAt(0)}`.toUpperCase();
  const style = { width: taille, height: taille, fontSize: Math.round(taille * 0.38) };
  return url && !casse
    ? <img className="avatar" style={style} src={url} alt={`Dr ${prenom} ${nom}`} onError={() => setCasse(true)} />
    : <span className="avatar avatar-ini" style={style}>{ini}</span>;
}

export function Etoiles({ moyenne = 0, total = 0 }) {
  if (!total) return <span className="doux">Pas encore d'avis</span>;
  const n = Math.round(moyenne);
  return (
    <span className="etoiles" title={`${moyenne} sur 5`}>
      {'★'.repeat(n)}<span className="etoiles-vide">{'★'.repeat(5 - n)}</span>{' '}
      <b>{String(moyenne).replace('.', ',')}</b> <span className="doux">({total} avis)</span>
    </span>
  );
}

export function Horaires({ horaires = [] }) {
  if (!horaires.length) return <p className="doux">Horaires non renseignés.</p>;
  return (
    <div className="horaires-grille">
      {horaires.map((h, i) => (
        <div key={i} className="horaire-ligne"><span>{JOURS[h.jour]}</span><b>{h.debut} – {h.fin}</b></div>
      ))}
    </div>
  );
}

/** Liste des avis + formulaire pour le patient connecté. */
export function AvisBloc({ proId, onChange }) {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [note, setNote] = useState(0);
  const [com, setCom] = useState('');
  const [msg, setMsg] = useState('');

  const charger = () => api.get(`/api/public/professionnels/${proId}/avis`)
    .then((r) => setData(r.data)).catch((e) => setMsg(msgErreur(e)));
  useEffect(() => { charger(); }, [proId]); // eslint-disable-line

  const envoyer = async () => {
    if (!note) return setMsg('Choisissez une note de 1 à 5.');
    try {
      await api.post(`/api/patient/professionnels/${proId}/avis`, { note, commentaire: com });
      setMsg('Merci, votre avis est enregistré.');
      setCom('');
      charger();
      if (onChange) onChange();
    } catch (e) { setMsg(msgErreur(e)); }
  };

  return (
    <div className="avis-bloc">
      {data && data.avis.length === 0 && <p className="doux">Aucun avis pour le moment.</p>}
      {data && data.avis.map((a, i) => (
        <div key={i} className="avis-ligne">
          <div><span className="etoiles">{'★'.repeat(a.note)}<span className="etoiles-vide">{'★'.repeat(5 - a.note)}</span></span>
            {' '}<b>{a.auteur}</b> <span className="doux">{a.dateAvis}</span></div>
          {a.commentaire && <p>{a.commentaire}</p>}
        </div>
      ))}
      {user?.role === 'PATIENT' ? (
        <div className="avis-form">
          <b>Votre avis</b>
          <div className="choix-note">
            {[1, 2, 3, 4, 5].map((n) => (
              <button key={n} type="button" className={'etoile-btn' + (n <= note ? ' actif' : '')}
                onClick={() => setNote(n)} aria-label={`${n} sur 5`}>★</button>
            ))}
          </div>
          <textarea placeholder="Un commentaire (facultatif)" maxLength={500} value={com} onChange={(e) => setCom(e.target.value)} />
          <button type="button" onClick={envoyer}>Publier mon avis</button>
        </div>
      ) : (
        <p className="doux">Connectez-vous en tant que patient pour donner votre avis.</p>
      )}
      {msg && <p className={/Erreur|injoignable|Choisissez/.test(msg) ? 'err' : 'ok'}>{msg}</p>}
    </div>
  );
}

/** Éditeur d'horaires pour l'espace professionnel : une plage par jour. */
export function EditeurHoraires() {
  const vide = () => JOURS.slice(1).map(() => ({ actif: false, debut: '08:00', fin: '17:00' }));
  const [jours, setJours] = useState(vide());
  const [msg, setMsg] = useState('');

  useEffect(() => {
    api.get('/api/pro/horaires').then((r) => {
      const j = vide();
      r.data.forEach((h) => { if (!j[h.jour - 1].actif) j[h.jour - 1] = { actif: true, debut: h.debut, fin: h.fin }; });
      setJours(j);
    }).catch(() => {});
  }, []);

  const maj = (i, k, v) => setJours(jours.map((j, idx) => (idx === i ? { ...j, [k]: v } : j)));

  const enregistrer = async () => {
    const liste = jours.map((j, i) => ({ ...j, jour: i + 1 })).filter((j) => j.actif)
      .map((j) => ({ jour: j.jour, debut: j.debut, fin: j.fin }));
    try { await api.put('/api/pro/horaires', liste); setMsg('Horaires enregistrés.'); }
    catch (e) { setMsg(msgErreur(e)); }
  };

  return (
    <div className="carte">
      <h2>Mes horaires</h2>
      {jours.map((j, i) => (
        <div key={i} className="horaire-edit">
          <label style={{ fontWeight: 400 }}>
            <input type="checkbox" checked={j.actif} onChange={(e) => maj(i, 'actif', e.target.checked)} /> {JOURS[i + 1]}
          </label>
          <input type="time" disabled={!j.actif} value={j.debut} onChange={(e) => maj(i, 'debut', e.target.value)} />
          <span>→</span>
          <input type="time" disabled={!j.actif} value={j.fin} onChange={(e) => maj(i, 'fin', e.target.value)} />
        </div>
      ))}
      <button type="button" onClick={enregistrer}>Enregistrer les horaires</button>
      {msg && <p className={/Erreur|injoignable|invalide|doit/.test(msg) ? 'err' : 'ok'}>{msg}</p>}
    </div>
  );
}
