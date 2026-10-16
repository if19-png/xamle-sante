import { useEffect, useState } from 'react';
import { Circle, MapContainer, Popup, TileLayer } from 'react-leaflet';
import api, { CENTRE, msgErreur } from '../api';

const SYMPTOMES = ['FIEVRE', 'DIARRHEE', 'TOUX', 'VOMISSEMENTS', 'ERUPTION', 'AUTRE'];
const couleur = (nb) => (nb >= 10 ? '#b71c1c' : nb >= 5 ? '#ef6c00' : '#fbc02d');

export default function Epidemio() {
  const [quartiers, setQuartiers] = useState([]);
  const [zones, setZones] = useState([]);
  const [alertes, setAlertes] = useState([]);
  const [filtre, setFiltre] = useState('');
  const [f, setF] = useState({ quartierId: '', symptome: 'FIEVRE', description: '' });
  const [msg, setMsg] = useState('');

  const charger = () => {
    api.get('/api/public/heatmap', { params: { jours: 14, ...(filtre && { symptome: filtre }) } })
      .then((r) => setZones(r.data)).catch(() => {});
  };
  useEffect(() => {
    api.get('/api/public/quartiers').then((r) => setQuartiers(r.data)).catch(() => {});
    api.get('/api/public/alertes').then((r) => setAlertes(r.data)).catch(() => {});
  }, []);
  useEffect(charger, [filtre]); // eslint-disable-line

  const signaler = async (e) => {
    e.preventDefault();
    try {
      await api.post('/api/public/signalements', { ...f, quartierId: Number(f.quartierId) });
      setMsg('Merci. Votre signalement est anonyme et sera vérifié par un modérateur médical.');
      setF({ ...f, description: '' });
    } catch (err) { setMsg(msgErreur(err)); }
  };

  return (
    <>
      <div className="carte">
        <h1>Alertes sanitaires</h1>
        {alertes.length === 0 && <p>Aucune alerte en cours.</p>}
        {alertes.map((a) => (
          <p key={a.id}><span className="badge">{a.niveau}</span> <b>{a.titre}</b>
            {a.quartier && <> ({a.quartier})</>} — {a.message}</p>
        ))}
      </div>

      <div className="carte">
        <h2>Carte de chaleur (14 derniers jours)</h2>
        <label>Symptôme</label>
        <select value={filtre} onChange={(e) => setFiltre(e.target.value)}>
          <option value="">Tous</option>
          {SYMPTOMES.map((s) => <option key={s}>{s}</option>)}
        </select>
        <p style={{ fontSize: '.8rem' }}>Données agrégées et anonymes. Un quartier n'apparaît qu'à partir de 3 signalements validés.</p>
      </div>
      <MapContainer center={[CENTRE.lat, CENTRE.lon]} zoom={13} className="carte-map">
        <TileLayer attribution="&copy; OpenStreetMap" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        {zones.map((z) => (
          <Circle key={z.quartierId} center={[z.latitude, z.longitude]} radius={350 + z.nb * 60}
            pathOptions={{ color: couleur(z.nb), fillColor: couleur(z.nb), fillOpacity: 0.45 }}>
            <Popup><b>{z.quartier}</b><br />{z.nb} signalements</Popup>
          </Circle>
        ))}
      </MapContainer>

      <form className="carte" onSubmit={signaler}>
        <h2>Signaler des symptômes (anonyme)</h2>
        <label>Quartier</label>
        <select required value={f.quartierId} onChange={(e) => setF({ ...f, quartierId: e.target.value })}>
          <option value="">Choisir…</option>
          {quartiers.map((q) => <option key={q.id} value={q.id}>{q.nom}</option>)}
        </select>
        <label>Symptôme</label>
        <select value={f.symptome} onChange={(e) => setF({ ...f, symptome: e.target.value })}>
          {SYMPTOMES.map((s) => <option key={s}>{s}</option>)}
        </select>
        <label>Précisions (facultatif, sans nom ni adresse)</label>
        <textarea maxLength={500} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} />
        <button type="submit">Envoyer</button>
        {msg && <p className={/Merci/.test(msg) ? 'ok' : 'err'}>{msg}</p>}
      </form>
    </>
  );
}
