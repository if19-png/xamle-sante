import { useEffect, useState } from 'react';
import api, { CENTRE, msgErreur } from '../api';

const VIDE = { specialiteId: '', etablissementId: '', bio: '', telephonePro: '', latitude: '', longitude: '', langueIds: [], consultationVideo: false };

export default function EspacePro() {
  const [profil, setProfil] = useState(null);
  const [f, setF] = useState(VIDE);
  const [specialites, setSpecialites] = useState([]);
  const [langues, setLangues] = useState([]);
  const [etabs, setEtabs] = useState([]);
  const [rdvs, setRdvs] = useState([]);
  const [msg, setMsg] = useState('');

  const charger = () => {
    api.get('/api/pro/mon-profil').then((r) => {
      setProfil(r.data);
      const langIds = (r.data.langues || '').split(', ').map((n) => langues.find((l) => l.nom === n)?.id).filter(Boolean);
      setF({ specialiteId: r.data.specialiteId, etablissementId: r.data.etablissementId || '', bio: r.data.bio || '',
        telephonePro: r.data.telephone || '', latitude: r.data.latitude ?? '', longitude: r.data.longitude ?? '',
        langueIds: langIds, consultationVideo: !!r.data.consultationVideo });
    }).catch(() => setProfil(null));
    api.get('/api/pro/rendez-vous').then((r) => setRdvs(r.data)).catch(() => {});
  };
  useEffect(() => {
    api.get('/api/public/specialites').then((r) => setSpecialites(r.data));
    api.get('/api/public/etablissements').then((r) => setEtabs(r.data));
    api.get('/api/public/langues').then((r) => { setLangues(r.data); });
  }, []);
  useEffect(() => { if (langues.length) charger(); }, [langues]); // eslint-disable-line

  const maj = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
  const basculerLangue = (id) => setF({ ...f, langueIds: f.langueIds.includes(id) ? f.langueIds.filter((x) => x !== id) : [...f.langueIds, id] });
  const localiser = () => navigator.geolocation?.getCurrentPosition(
    (p) => setF({ ...f, latitude: p.coords.latitude, longitude: p.coords.longitude }),
    () => setF({ ...f, latitude: CENTRE.lat, longitude: CENTRE.lon }));

  const enregistrer = async (e) => {
    e.preventDefault();
    try {
      await api.put('/api/pro/mon-profil', {
        specialiteId: Number(f.specialiteId), etablissementId: f.etablissementId ? Number(f.etablissementId) : null,
        bio: f.bio, telephonePro: f.telephonePro, latitude: f.latitude === '' ? null : Number(f.latitude),
        longitude: f.longitude === '' ? null : Number(f.longitude), langueIds: f.langueIds, consultationVideo: f.consultationVideo,
      });
      setMsg('Profil enregistré (brouillon). Soumettez-le pour validation.');
      charger();
    } catch (err) { setMsg(msgErreur(err)); }
  };
  const soumettre = async () => {
    try { await api.post('/api/pro/mon-profil/soumettre'); setMsg('Profil soumis à l\'administration.'); charger(); }
    catch (err) { setMsg(msgErreur(err)); }
  };
  const confirmer = async (id) => {
    try { await api.post(`/api/pro/rendez-vous/${id}/confirmer`); charger(); } catch (err) { setMsg(msgErreur(err)); }
  };

  return (
    <>
      <div className="carte">
        <h1>Mon profil professionnel</h1>
        {profil ? <p>Statut : <span className={`badge ${profil.statutValidation}`}>{profil.statutValidation}</span>
          {profil.motifRejet && <span className="err"> — Motif du rejet : {profil.motifRejet}</span>}</p>
          : <p>Vous n'avez pas encore de profil. Remplissez le formulaire.</p>}
        {msg && <p className="ok">{msg}</p>}
      </div>
      <form className="carte" onSubmit={enregistrer}>
        <label>Spécialité</label>
        <select required value={f.specialiteId} onChange={maj('specialiteId')}>
          <option value="">Choisir…</option>{specialites.map((s) => <option key={s.id} value={s.id}>{s.nom}</option>)}
        </select>
        <label>Établissement</label>
        <select value={f.etablissementId} onChange={maj('etablissementId')}>
          <option value="">Aucun / cabinet privé</option>{etabs.map((e) => <option key={e.id} value={e.id}>{e.nom}</option>)}
        </select>
        <label>Présentation</label><textarea value={f.bio} onChange={maj('bio')} />
        <label>Téléphone professionnel</label><input value={f.telephonePro} onChange={maj('telephonePro')} />
        <div className="grille">
          <div><label>Latitude</label><input value={f.latitude} onChange={maj('latitude')} /></div>
          <div><label>Longitude</label><input value={f.longitude} onChange={maj('longitude')} /></div>
        </div>
        <button type="button" className="sec" onClick={localiser}>Utiliser ma position</button>
        <label>Langues parlées</label>
        {langues.map((l) => (
          <label key={l.id} style={{ fontWeight: 400 }}>
            <input type="checkbox" checked={f.langueIds.includes(l.id)} onChange={() => basculerLangue(l.id)} /> {l.nom}
          </label>
        ))}
        <label><input type="checkbox" checked={f.consultationVideo} onChange={maj('consultationVideo')} /> Je propose la consultation vidéo</label>
        <button type="submit">Enregistrer</button>
        {profil && ['BROUILLON', 'REJETE'].includes(profil.statutValidation) &&
          <button type="button" className="sec" onClick={soumettre}>Soumettre à la validation</button>}
      </form>

      <div className="carte">
        <h2>Mes rendez-vous</h2>
        {rdvs.length === 0 && <p>Aucun rendez-vous.</p>}
        {rdvs.map((r) => (
          <p key={r.id}>{new Date(r.dateHeure).toLocaleString('fr-FR')} — {r.patient} — {r.type}{' '}
            <span className={`badge ${r.statut}`}>{r.statut}</span>
            {r.statut === 'DEMANDE' && r.type === 'PRESENTIEL' && <button onClick={() => confirmer(r.id)}>Confirmer</button>}</p>
        ))}
      </div>
    </>
  );
}
