import { useEffect, useState } from 'react';
import api, { msgErreur } from '../api';
import { useAuth } from '../auth';

export default function Campagnes() {
  const { user } = useAuth();
  const [liste, setListe] = useState([]);
  const [msg, setMsg] = useState('');

  useEffect(() => { api.get('/api/public/campagnes').then((r) => setListe(r.data)).catch(() => {}); }, []);

  const sAbonner = async (id) => {
    try {
      await api.post(`/api/campagnes/${id}/abonnement`);
      setMsg('Abonnement enregistré : vous recevrez un SMS 24 h avant (vérifiez votre numéro dans votre compte).');
    } catch (e) { setMsg(msgErreur(e)); }
  };

  return (
    <>
      <div className="carte"><h1>Agenda santé</h1>
        <p>Campagnes de vaccination, de dépistage et de sensibilisation.</p>
        {msg && <p className="ok">{msg}</p>}</div>
      {liste.length === 0 && <p className="carte">Aucune campagne à venir.</p>}
      {liste.map((c) => (
        <div className="carte" key={c.id}>
          <h2>{c.titre} <span className="badge">{c.type}</span></h2>
          <p>{new Date(c.dateDebut).toLocaleString('fr-FR')}{c.lieu && <> — {c.lieu}</>}</p>
          {c.description && <p>{c.description}</p>}
          {user ? <button onClick={() => sAbonner(c.id)}>Me rappeler par SMS</button>
            : <p><i>Connectez-vous pour recevoir un rappel.</i></p>}
        </div>
      ))}
    </>
  );
}
