import { useState } from 'react';
import api, { msgErreur, CENTRE } from '../api';

export default function Sos() {
  const [tel, setTel] = useState(localStorage.getItem('sosTel') || '');
  const [etat, setEtat] = useState(null);
  const [erreur, setErreur] = useState('');
  const [charge, setCharge] = useState(false);
  const [sansGps, setSansGps] = useState(false);

  const envoyer = async (lat, lon, t0) => {
    try {
      const { data } = await api.post('/api/public/sos', {
        latitude: lat, longitude: lon, telephoneDestinataire: tel,
      });
      setEtat({ ...data, ms: Math.round(performance.now() - t0) });
    } catch (e) { setErreur(msgErreur(e)); } finally { setCharge(false); }
  };

  const declencher = () => {
    setErreur('');
    setEtat(null);
    setSansGps(false);
    if (!tel.trim()) return setErreur('Saisissez d\'abord le numéro qui recevra le SMS (proche, famille).');
    localStorage.setItem('sosTel', tel);
    if (!navigator.geolocation) { setSansGps(true); return setErreur('Géolocalisation non disponible sur cet appareil.'); }
    setCharge(true);
    const t0 = performance.now();
    navigator.geolocation.getCurrentPosition(
      (p) => envoyer(p.coords.latitude, p.coords.longitude, t0),
      (err) => {
        setCharge(false);
        setSansGps(true);
        setErreur('Position introuvable : ' + err.message + '. Autorisez la localisation dans le navigateur.');
      },
      { enableHighAccuracy: false, timeout: 15000, maximumAge: 300000 }
    );
  };

  // Secours (démo / ordinateur sans GPS) : centre de Ziguinchor, position approximative.
  const declencherApprox = () => {
    setErreur('');
    setSansGps(false);
    setCharge(true);
    envoyer(CENTRE.lat, CENTRE.lon, performance.now());
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
        {sansGps && (
          <button onClick={declencherApprox} disabled={charge}>
            Utiliser une position approximative (centre de Ziguinchor)
          </button>
        )}
      </div>
      {etat && (
        <>
          <div className={'carte sos-statut ' + (etat.smsEnvoye ? 'ok-bande' : 'ko-bande')}>
            <div className="sos-statut-icone">{etat.smsEnvoye ? '✓' : '!'}</div>
            <div>
              <b>{!etat.smsEnvoye ? 'Alerte non envoyée' : etat.simule ? 'Alerte enregistrée' : 'Alerte envoyée'}</b>
              {etat.smsEnvoye && etat.simule && (
                <div className="doux">Votre position et les urgences proches sont enregistrées. Envoi du SMS simulé (mode démonstration).</div>
              )}
              <div className="doux">Traitée en {etat.ms} ms</div>
              {etat.erreur && <div className="err">{etat.erreur}</div>}
            </div>
          </div>

          <div className="carte">
            <h2>{etat.simule ? 'Message préparé pour votre proche' : 'Message transmis à votre proche'}</h2>
            <div className="bulle-sms">{etat.message}</div>
          </div>

          <h2 className="sos-titre">Urgences les plus proches</h2>
          {etat.urgences.map((u, i) => (
            <div className="carte sos-urgence" key={i}>
              <div>
                <b>{u.nom}</b>
                <div className="doux">{(u.serviceType || '').split(', ').join(' · ')}</div>
              </div>
              <div className="sos-droite">
                <span className="badge">{(u.distanceM / 1000).toFixed(1)} km</span>
                {u.telephone
                  ? <a className="btn-appel" href={`tel:${u.telephone}`}>Appeler</a>
                  : <span className="doux">N° non renseigné</span>}
              </div>
            </div>
          ))}
        </>
      )}
    </>
  );
}
