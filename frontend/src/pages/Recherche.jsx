import { useEffect, useState } from 'react';
import { Circle, MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet';
import api, { CENTRE, msgErreur } from '../api';
import { useAuth } from '../auth';

function Recentrer({ pos }) {
  const map = useMap();
  useEffect(() => { map.setView([pos.lat, pos.lon], 13); }, [pos.lat, pos.lon, map]);
  return null;
}

export default function Recherche() {
  const { user } = useAuth();
  const [pos, setPos] = useState(CENTRE);
  const [geo, setGeo] = useState(false);
  const [specialites, setSpecialites] = useState([]);
  const [langues, setLangues] = useState([]);
  const [f, setF] = useState({ specialiteId: '', langueId: '', q: '', rayonKm: '', disponible: false });
  const [res, setRes] = useState([]);
  const [msg, setMsg] = useState('');
  const [rdv, setRdv] = useState(null);
  const [rdvForm, setRdvForm] = useState({ dateHeure: '', type: 'PRESENTIEL', motif: '' });

  useEffect(() => {
    api.get('/api/public/specialites').then((r) => setSpecialites(r.data)).catch(() => {});
    api.get('/api/public/langues').then((r) => setLangues(r.data)).catch(() => {});
  }, []);

  const chercher = () => {
    const params = { lat: pos.lat, lon: pos.lon };
    if (f.specialiteId) params.specialiteId = f.specialiteId;
    if (f.langueId) params.langueId = f.langueId;
    if (f.q) params.q = f.q;
    if (f.rayonKm) params.rayonM = Number(f.rayonKm) * 1000;
    if (f.disponible) params.disponible = true;
    api.get('/api/public/professionnels', { params })
      .then((r) => { setRes(r.data); setMsg(r.data.length ? '' : 'Aucun spécialiste trouvé avec ces critères.'); })
      .catch((e) => setMsg(msgErreur(e)));
  };
  useEffect(() => { chercher(); }, [pos]); // eslint-disable-line

  const localiser = () => {
    if (!navigator.geolocation) return setMsg('Géolocalisation non disponible sur cet appareil.');
    navigator.geolocation.getCurrentPosition(
      (p) => { setPos({ lat: p.coords.latitude, lon: p.coords.longitude }); setGeo(true); },
      () => setMsg('Position indisponible : résultats triés depuis le centre de Ziguinchor.'),
      { timeout: 5000, maximumAge: 60000 }
    );
  };

  const reserver = async (e) => {
    e.preventDefault();
    try {
      const { data } = await api.post('/api/patient/rendez-vous', { professionnelId: rdv.id, ...rdvForm });
      setMsg(data.type === 'VIDEO'
        ? 'Demande créée. Réglez les frais de dossier (1 000 FCFA) dans « Mes RDV » pour confirmer la visio.'
        : 'Demande envoyée. Le professionnel doit la confirmer (voir « Mes RDV »).');
      setRdv(null);
    } catch (err) { setMsg(msgErreur(err)); }
  };

  const maj = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  return (
    <>
      <div className="carte">
        <h1>Trouver un spécialiste</h1>
        <div className="grille">
          <div><label>Spécialité</label>
            <select value={f.specialiteId} onChange={maj('specialiteId')}>
              <option value="">Toutes</option>
              {specialites.map((s) => <option key={s.id} value={s.id}>{s.nom}</option>)}
            </select></div>
          <div><label>Langue parlée</label>
            <select value={f.langueId} onChange={maj('langueId')}>
              <option value="">Toutes</option>
              {langues.map((l) => <option key={l.id} value={l.id}>{l.nom}</option>)}
            </select></div>
          <div><label>Nom, spécialité, établissement</label><input value={f.q} onChange={maj('q')} /></div>
          <div><label>Rayon (km)</label><input type="number" min="1" value={f.rayonKm} onChange={maj('rayonKm')} /></div>
        </div>
        <label><input type="checkbox" checked={f.disponible} onChange={maj('disponible')} /> Disponible maintenant</label>
        <button onClick={chercher}>Rechercher</button>
        <button className="sec" onClick={localiser}>{geo ? 'Position utilisée ✓' : 'Utiliser ma position'}</button>
        {msg && <p className={/Erreur|injoignable/.test(msg) ? 'err' : 'ok'}>{msg}</p>}
      </div>

      <MapContainer center={[pos.lat, pos.lon]} zoom={13} className="carte-map">
        <TileLayer attribution="&copy; OpenStreetMap" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <Recentrer pos={pos} />
        <Circle center={[pos.lat, pos.lon]} radius={80} pathOptions={{ color: '#1565c0' }} />
        {res.filter((r) => r.latitude != null).map((r) => (
          <Marker key={r.id} position={[r.latitude, r.longitude]}>
            <Popup>
              <b>Dr {r.prenom} {r.nom}</b><br />{r.specialite}<br />{r.etablissement}
              {r.distanceM != null && <><br />{(r.distanceM / 1000).toFixed(1)} km</>}
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      {res.map((r) => (
        <div className="carte" key={r.id}>
          <h2>Dr {r.prenom} {r.nom} <span className="badge">{r.specialite}</span></h2>
          <p>{r.etablissement || 'Établissement non précisé'}
            {r.distanceM != null && <> — <b>{(r.distanceM / 1000).toFixed(1)} km</b></>}</p>
          {r.langues && <p>Langues : {r.langues}</p>}
          {r.telephone && <p>Tél : {r.telephone}</p>}
          {r.bio && <p>{r.bio}</p>}
          {r.consultationVideo && <span className="badge">Consultation vidéo</span>}
          {user?.role === 'PATIENT' && <div><button onClick={() => setRdv(r)}>Prendre rendez-vous</button></div>}
        </div>
      ))}

      {rdv && (
        <form className="carte" onSubmit={reserver}>
          <h2>Rendez-vous avec Dr {rdv.prenom} {rdv.nom}</h2>
          <label>Date et heure</label>
          <input type="datetime-local" required value={rdvForm.dateHeure} onChange={(e) => setRdvForm({ ...rdvForm, dateHeure: e.target.value })} />
          <label>Type</label>
          <select value={rdvForm.type} onChange={(e) => setRdvForm({ ...rdvForm, type: e.target.value })}>
            <option value="PRESENTIEL">En présentiel</option>
            {rdv.consultationVideo && <option value="VIDEO">Visio (1 000 FCFA de frais de dossier)</option>}
          </select>
          <label>Motif</label>
          <textarea value={rdvForm.motif} onChange={(e) => setRdvForm({ ...rdvForm, motif: e.target.value })} />
          <button type="submit">Envoyer la demande</button>
          <button type="button" className="sec" onClick={() => setRdv(null)}>Annuler</button>
        </form>
      )}
      {!user && <p className="carte">Connectez-vous en tant que patient pour prendre rendez-vous.</p>}
    </>
  );
}
