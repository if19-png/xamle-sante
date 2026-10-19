import { useEffect, useMemo, useState } from 'react';
import L from 'leaflet';
import { CircleMarker, MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet';
import api, { CENTRE, msgErreur } from '../api';

const TYPES = {
  HOPITAL: { libelle: 'Hôpitaux', un: 'Hôpital', couleur: '#d32f2f', lettre: 'H' },
  CLINIQUE: { libelle: 'Cliniques', un: 'Clinique', couleur: '#0e9f8e', lettre: 'C' },
  DISPENSAIRE: { libelle: 'Dispensaires', un: 'Dispensaire', couleur: '#0b7a5b', lettre: 'D' },
  PHARMACIE: { libelle: 'Pharmacies', un: 'Pharmacie', couleur: '#2563eb', lettre: 'P' },
};

const icone = (type, actif) => L.divIcon({
  className: '',
  html: `<span class="pin${actif ? ' pin-actif' : ''}" style="--c:${(TYPES[type] || TYPES.HOPITAL).couleur}"><b>${(TYPES[type] || TYPES.HOPITAL).lettre}</b></span>`,
  iconSize: [30, 30],
  iconAnchor: [15, 36],
  popupAnchor: [0, -34],
});

function Vol({ cible }) {
  const map = useMap();
  useEffect(() => {
    if (cible) map.flyTo([cible.latitude, cible.longitude], 15, { duration: 0.6 });
  }, [cible, map]);
  return null;
}

function Recentrer({ centre }) {
  const map = useMap();
  useEffect(() => { map.setView(centre, 13); }, [centre[0], centre[1]]); // eslint-disable-line
  return null;
}

const km = (m) => (m == null ? null : (m / 1000).toFixed(1).replace('.', ','));
const telLien = (t) => `tel:${t.replace(/\s/g, '')}`;
const itineraire = (e) => `https://www.google.com/maps/dir/?api=1&destination=${e.latitude},${e.longitude}`;

export default function Etablissements() {
  const [liste, setListe] = useState([]);
  const [charge, setCharge] = useState(true);
  const [erreur, setErreur] = useState('');
  const [info, setInfo] = useState('');
  const [pos, setPos] = useState({ ...CENTRE, reel: false });
  const [type, setType] = useState('TOUS');
  const [h24, setH24] = useState(false);
  const [q, setQ] = useState('');
  const [choisi, setChoisi] = useState(null);

  const charger = (p) => {
    setCharge(true);
    setErreur('');
    api.get('/api/public/etablissements', { params: { lat: p.lat, lon: p.lon }, timeout: 60000 })
      .then((r) => setListe(r.data))
      .catch((e) => setErreur(msgErreur(e)))
      .finally(() => setCharge(false));
  };

  const localiser = () => {
    setInfo('');
    if (!navigator.geolocation) return setInfo('Géolocalisation indisponible : distances calculées depuis le centre de Ziguinchor.');
    navigator.geolocation.getCurrentPosition(
      (g) => {
        const p = { lat: g.coords.latitude, lon: g.coords.longitude, reel: true };
        setPos(p);
        charger(p);
      },
      () => setInfo('Position introuvable : distances calculées depuis le centre de Ziguinchor.'),
      { enableHighAccuracy: false, timeout: 15000, maximumAge: 300000 }
    );
  };

  useEffect(() => { charger(pos); localiser(); }, []); // eslint-disable-line

  const comptes = useMemo(() => {
    const c = { TOUS: liste.length };
    liste.forEach((e) => { c[e.type] = (c[e.type] || 0) + 1; });
    return c;
  }, [liste]);

  const visibles = useMemo(() => {
    const t = q.trim().toLowerCase();
    return liste.filter((e) =>
      (type === 'TOUS' || e.type === type)
      && (!h24 || e.ouvert24h)
      && (!t || `${e.nom} ${e.adresse || ''} ${e.services || ''}`.toLowerCase().includes(t)));
  }, [liste, type, h24, q]);

  const cible = visibles.find((e) => e.id === choisi) || null;
  // On ne recentre sur la position réelle que si elle est dans la région (sinon on reste sur Ziguinchor).
  const proche = pos.reel && Math.hypot(pos.lat - CENTRE.lat, pos.lon - CENTRE.lon) < 0.6;
  const centre = proche ? [pos.lat, pos.lon] : [CENTRE.lat, CENTRE.lon];

  const choisir = (e) => {
    setChoisi(e.id);
    document.getElementById(`etab-${e.id}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  };

  return (
    <>
      <h1>Établissements de santé</h1>
      <p className="doux">Hôpitaux, cliniques, dispensaires et pharmacies de Ziguinchor : services, horaires et itinéraire.</p>

      <div className="carte etab-filtres">
        <input placeholder="Rechercher un nom, un quartier, un service (maternité, vaccination…)"
          value={q} onChange={(e) => setQ(e.target.value)} />
        <div className="etab-chips">
          {['TOUS', ...Object.keys(TYPES)].map((t) => (
            <button key={t} type="button" className={'chip' + (type === t ? ' chip-actif' : '')} onClick={() => setType(t)}>
              {t === 'TOUS' ? 'Tous' : TYPES[t].libelle} <span className="chip-n">{comptes[t] || 0}</span>
            </button>
          ))}
          <button type="button" className={'chip chip-24' + (h24 ? ' chip-actif' : '')} onClick={() => setH24(!h24)}>
            Ouvert 24h/24
          </button>
          <button type="button" className="chip" onClick={localiser}>Me localiser</button>
        </div>
        {info && <p className="doux">{info}</p>}
      </div>

      {charge && <p className="carte">Chargement… (le serveur peut mettre une minute à se réveiller)</p>}
      {erreur && <div className="carte"><p className="err">{erreur}</p><button onClick={() => charger(pos)}>Réessayer</button></div>}

      {!charge && !erreur && (
        <div className="etab-grille">
          <div className="etab-map-boite">
            <MapContainer center={centre} zoom={13} className="etab-map">
              <TileLayer attribution="&copy; OpenStreetMap" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
              <Recentrer centre={centre} />
              <Vol cible={cible} />
              {pos.reel && <CircleMarker center={[pos.lat, pos.lon]} radius={8}
                pathOptions={{ color: '#fff', weight: 3, fillColor: '#2563eb', fillOpacity: 1 }}><Popup>Vous êtes ici</Popup></CircleMarker>}
              {visibles.map((e) => (
                <Marker key={e.id} position={[e.latitude, e.longitude]} icon={icone(e.type, e.id === choisi)}
                  eventHandlers={{ click: () => choisir(e) }}>
                  <Popup><b>{e.nom}</b><br />{(TYPES[e.type] || {}).un} · {e.adresse}</Popup>
                </Marker>
              ))}
            </MapContainer>
            <div className="etab-legende">
              {Object.entries(TYPES).map(([k, v]) => (
                <span key={k}><i style={{ background: v.couleur }} />{v.un}</span>
              ))}
            </div>
          </div>

          <div className="etab-liste">
            {visibles.length === 0 && <p className="carte">Aucun établissement ne correspond à ces critères.</p>}
            {visibles.map((e) => {
              const t = TYPES[e.type] || TYPES.HOPITAL;
              return (
                <article key={e.id} id={`etab-${e.id}`} className={'carte etab' + (e.id === choisi ? ' etab-actif' : '')}
                  onClick={() => setChoisi(e.id)}>
                  <div className="etab-tete">
                    <span className="etab-pin" style={{ background: t.couleur }}>{t.lettre}</span>
                    <div className="etab-titre">
                      <h2>{e.nom}</h2>
                      <span className="doux">{t.un} · {e.adresse}</span>
                    </div>
                    {e.distanceM != null && <span className="badge">{km(e.distanceM)} km</span>}
                  </div>
                  {e.services && (
                    <div className="etab-puces">
                      {e.services.split(',').map((s) => s.trim()).filter(Boolean).map((s) => <span key={s} className="puce">{s}</span>)}
                    </div>
                  )}
                  <p className="etab-horaires">
                    {e.ouvert24h && <span className="badge badge-24">24h/24</span>} {e.horaires || 'Horaires non renseignés'}
                  </p>
                  <div className="etab-actions">
                    {e.telephone
                      ? <a className="btn-appel" href={telLien(e.telephone)}>Appeler · {e.telephone}</a>
                      : <span className="doux">N° non renseigné</span>}
                    <a className="btn-lien" href={itineraire(e)} target="_blank" rel="noreferrer">Itinéraire</a>
                  </div>
                </article>
              );
            })}
          </div>
        </div>
      )}
    </>
  );
}
