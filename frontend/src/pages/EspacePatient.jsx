import { useEffect, useState } from 'react';
import api, { msgErreur } from '../api';

export default function EspacePatient() {
  const [rdvs, setRdvs] = useState([]);
  const [msg, setMsg] = useState('');
  const charger = () => api.get('/api/patient/rendez-vous').then((r) => setRdvs(r.data)).catch((e) => setMsg(msgErreur(e)));
  useEffect(() => { charger(); }, []);

  const payer = async (id) => {
    try {
      await api.post(`/api/patient/paiements/${id}/payer`);
      setMsg('Paiement Wave (sandbox simulé) réussi. Rendez-vous confirmé.');
      charger();
    } catch (e) { setMsg(msgErreur(e)); }
  };

  return (
    <>
      <div className="carte"><h1>Mes rendez-vous</h1>{msg && <p className="ok">{msg}</p>}</div>
      {rdvs.length === 0 && <p className="carte">Aucun rendez-vous. Utilisez l'annuaire pour en demander un.</p>}
      {rdvs.map((r) => (
        <div className="carte" key={r.id}>
          <h2>Dr {r.professionnel} <span className={`badge ${r.statut}`}>{r.statut}</span></h2>
          <p>{new Date(r.dateHeure).toLocaleString('fr-FR')} — {r.type}</p>
          {r.motif && <p>{r.motif}</p>}
          {r.paiementId && (
            <p>Frais de dossier : {r.montant} FCFA <span className={`badge ${r.paiementStatut}`}>{r.paiementStatut}</span></p>
          )}
          {r.paiementStatut === 'EN_ATTENTE' && (
            <button onClick={() => payer(r.paiementId)}>Payer {r.montant} FCFA (Wave simulé)</button>
          )}
          {r.lienVideo && <p><a href={r.lienVideo} target="_blank" rel="noreferrer">Rejoindre la visioconsultation</a></p>}
        </div>
      ))}
    </>
  );
}
