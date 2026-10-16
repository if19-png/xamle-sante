import { useState } from 'react';
import api, { msgErreur } from '../api';

export default function Sos() {
  const [tel, setTel] = useState(localStorage.getItem('sosTel') || '');
  const [etat, setEtat] = useState(null);
  const [erreur, setErreur] = useState('');
  const [charge, setCharge] = useState(false);

  const declencher = () => {
    setErreur('');
    setEtat(null);
    if (!tel.trim()) return setErreur('Saisissez d\'abord le numéro qui recevra le SMS (proche, famille).');
    if (!navigator.geolocation) return setErreur('Géolocalisation non disponible sur cet appareil.');
    localStorage.setItem('sosTel', tel);
    setCharge(true);
    const t0 = performance.now();
    navigator.geolocation.getCurrentPosition(
      async (p) => {
        try {
          const { data } = await api.post('/api/public/sos', {
            latitude: p.coords.latitude, longitude: p.coords.longitude, telephoneDestinataire: tel,
          });
          setEtat({ ...data, ms: Math.round(performance.now() - t0) });
        } catch (e) { setErreur(msgErreur(e)); } finally { setCharge(false); }
      },
      (err) => { setCharge(false); setErreur('Position introuvable : ' + err.message + '. Activez la localisation.'); },
      { enableHighAccuracy: false, timeout: 3000, maximumAge: 120000 }
    );
  };

  return (
    <>
      <div className="carte">
        <h1>Urgence</h1>
        <label>Numéro à prévenir par SMS</label>
        <input value={tel} onChange={(e) => setTel(e.target.value)} placeholder="77 123 45 67" inputMode="tel" />
        <button className="sos-bouton" onClick={declencher} disabled={charge}>
          {charge ? 'Localisation…' : 'SOS'}
        </button>
        {erreur && <p className="err">{erreur}</p>}
      </div>
      {etat && (
        <div className="carte">
          <h2>{etat.smsEnvoye ? '✅ SMS envoyé' : '⚠️ SMS non envoyé'} <span className="badge">{etat.ms} ms</span></h2>
          {etat.erreur && <p className="err">{etat.erreur}</p>}
          <p><i>{etat.message}</i></p>
          <h2>Urgences les plus proches</h2>
          {etat.urgences.map((u, i) => (
            <p key={i}><b>{u.nom}</b> ({u.serviceType}) — {(u.distanceM / 1000).toFixed(1)} km
              {u.telephone && <> — <a href={`tel:${u.telephone}`}>{u.telephone}</a></>}</p>
          ))}
        </div>
      )}
    </>
  );
}
