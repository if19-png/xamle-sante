import { useEffect, useState } from 'react';
import api, { msgErreur } from '../api';
import { useAuth } from '../auth';

function Validation() {
  const [liste, setListe] = useState([]);
  const [msg, setMsg] = useState('');
  const charger = () => api.get('/api/admin/professionnels/en-attente').then((r) => setListe(r.data)).catch((e) => setMsg(msgErreur(e)));
  useEffect(() => { charger(); }, []);
  const agir = async (id, action) => {
    try {
      if (action === 'rejeter') {
        const motif = window.prompt('Motif du rejet ?');
        if (!motif) return;
        await api.post(`/api/admin/professionnels/${id}/rejeter`, { motif });
      } else await api.post(`/api/admin/professionnels/${id}/valider`);
      charger();
    } catch (e) { setMsg(msgErreur(e)); }
  };
  return (
    <div className="carte"><h2>Profils en attente de validation</h2>{msg && <p className="err">{msg}</p>}
      {liste.length === 0 && <p>Aucun profil en attente.</p>}
      {liste.map((p) => (
        <div key={p.id}><b>Dr {p.prenom} {p.nom}</b> — {p.specialite} — {p.etablissement || 'sans établissement'}
          <p>{p.bio}</p>
          <button onClick={() => agir(p.id, 'valider')}>Valider</button>
          <button className="danger" onClick={() => agir(p.id, 'rejeter')}>Rejeter</button><hr /></div>
      ))}
    </div>
  );
}

function Moderation() {
  const [liste, setListe] = useState([]);
  const [msg, setMsg] = useState('');
  const charger = () => api.get('/api/moderation/signalements').then((r) => setListe(r.data)).catch((e) => setMsg(msgErreur(e)));
  useEffect(() => { charger(); }, []);
  const agir = async (id, action) => { try { await api.post(`/api/moderation/signalements/${id}/${action}`); charger(); } catch (e) { setMsg(msgErreur(e)); } };
  return (
    <div className="carte"><h2>Signalements à modérer</h2>{msg && <p className="err">{msg}</p>}
      {liste.length === 0 && <p>Rien à modérer.</p>}
      <div className="defile"><table><tbody>
        {liste.map((s) => (
          <tr key={s.id}><td>{s.quartier}</td><td>{s.symptome}</td><td>{s.description}</td>
            <td><button onClick={() => agir(s.id, 'valider')}>Valider</button>
              <button className="danger" onClick={() => agir(s.id, 'rejeter')}>Rejeter</button></td></tr>
        ))}
      </tbody></table></div>
    </div>
  );
}

function Dashboard() {
  const [d, setD] = useState(null);
  const [msg, setMsg] = useState('');
  useEffect(() => { api.get('/api/admin/dashboard').then((r) => setD(r.data)).catch((e) => setMsg(msgErreur(e))); }, []);
  const exporter = async () => {
    try {
      const { data } = await api.get('/api/admin/export/signalements.csv', { responseType: 'blob' });
      const url = URL.createObjectURL(data);
      const a = document.createElement('a'); a.href = url; a.download = 'signalements.csv'; a.click(); URL.revokeObjectURL(url);
    } catch (e) { setMsg(msgErreur(e)); }
  };
  if (!d) return <p className="carte">{msg || 'Chargement…'}</p>;
  const Bloc = ({ titre, lignes }) => (
    <div className="carte"><h2>{titre}</h2>
      {lignes.length === 0 ? <p>Aucune donnée.</p> : lignes.map((l) => <p key={l.cle}>{l.cle} : <b>{l.nb}</b></p>)}</div>
  );
  return (
    <>
      <div className="carte"><h2>Zones à risque ({d.periodeJours} derniers jours)</h2>
        {d.zonesARisque.length === 0 ? <p>Aucune zone au-dessus du seuil de risque.</p>
          : d.zonesARisque.map((z) => <p key={z.quartierId}><b>{z.quartier}</b> : {z.nb} signalements validés</p>)}
        <button onClick={exporter}>Exporter en CSV</button>{msg && <p className="err">{msg}</p>}</div>
      <div className="grille">
        <Bloc titre="Professionnels" lignes={d.professionnelsParStatut} />
        <Bloc titre="Signalements" lignes={d.signalementsParStatut} />
        <Bloc titre="Par symptôme (validés)" lignes={d.signalementsParSymptome} />
        <Bloc titre="Rendez-vous" lignes={d.rendezVousParStatut} />
        <Bloc titre="Paiements" lignes={d.paiementsParStatut} />
        <Bloc titre="Rappels SMS" lignes={d.rappelsParStatut} />
      </div>
      <div className="carte"><h2>SOS</h2><p>Déclenchés : <b>{d.sosTotal}</b> — SMS envoyés : <b>{d.sosSmsEnvoyes}</b></p></div>
    </>
  );
}

function Publication() {
  const [alerte, setAlerte] = useState({ niveau: 'INFO', titre: '', message: '' });
  const [camp, setCamp] = useState({ type: 'VACCINATION', titre: '', lieu: '', description: '', dateDebut: '' });
  const [rappels, setRappels] = useState('');
  const [msg, setMsg] = useState('');
  const publierAlerte = async (e) => { e.preventDefault(); try { await api.post('/api/admin/alertes', alerte); setMsg('Alerte publiée.'); } catch (x) { setMsg(msgErreur(x)); } };
  const creerCampagne = async (e) => { e.preventDefault(); try { await api.post('/api/admin/campagnes', camp); setMsg('Campagne créée.'); } catch (x) { setMsg(msgErreur(x)); } };
  const declencher = async () => { try { const { data } = await api.post('/api/admin/rappels/declencher'); setRappels(`${data.traites} rappel(s) traité(s).`); } catch (x) { setRappels(msgErreur(x)); } };
  return (
    <>
      {msg && <p className="carte ok">{msg}</p>}
      <form className="carte" onSubmit={publierAlerte}><h2>Publier une alerte</h2>
        <select value={alerte.niveau} onChange={(e) => setAlerte({ ...alerte, niveau: e.target.value })}>
          <option>INFO</option><option>VIGILANCE</option><option>ALERTE</option></select>
        <label>Titre</label><input required value={alerte.titre} onChange={(e) => setAlerte({ ...alerte, titre: e.target.value })} />
        <label>Message</label><textarea required value={alerte.message} onChange={(e) => setAlerte({ ...alerte, message: e.target.value })} />
        <button type="submit">Publier</button></form>
      <form className="carte" onSubmit={creerCampagne}><h2>Créer une campagne</h2>
        <select value={camp.type} onChange={(e) => setCamp({ ...camp, type: e.target.value })}>
          <option>VACCINATION</option><option>DEPISTAGE</option><option>SENSIBILISATION</option></select>
        <label>Titre</label><input required value={camp.titre} onChange={(e) => setCamp({ ...camp, titre: e.target.value })} />
        <label>Lieu</label><input value={camp.lieu} onChange={(e) => setCamp({ ...camp, lieu: e.target.value })} />
        <label>Date et heure de début</label>
        <input type="datetime-local" required value={camp.dateDebut} onChange={(e) => setCamp({ ...camp, dateDebut: e.target.value })} />
        <button type="submit">Créer</button></form>
      <div className="carte"><h2>Rappels SMS</h2>
        <p>La tâche planifiée tourne toute les minutes. Ce bouton la déclenche immédiatement (démo).</p>
        <button onClick={declencher}>Envoyer les rappels dus</button>{rappels && <p className="ok">{rappels}</p>}</div>
    </>
  );
}

function Utilisateurs() {
  const [f, setF] = useState({ email: '', motDePasse: '', nom: '', prenom: '', telephone: '', role: 'MODERATEUR' });
  const [msg, setMsg] = useState('');
  const creer = async (e) => { e.preventDefault(); try { await api.post('/api/admin/utilisateurs', f); setMsg('Compte créé.'); } catch (x) { setMsg(msgErreur(x)); } };
  const maj = (k) => (e) => setF({ ...f, [k]: e.target.value });
  return (
    <form className="carte" onSubmit={creer}><h2>Créer un compte (modérateur, admin…)</h2>
      <select value={f.role} onChange={maj('role')}><option>MODERATEUR</option><option>ADMIN</option></select>
      <div className="grille"><div><label>Prénom</label><input required value={f.prenom} onChange={maj('prenom')} /></div>
        <div><label>Nom</label><input required value={f.nom} onChange={maj('nom')} /></div></div>
      <label>E-mail</label><input type="email" required value={f.email} onChange={maj('email')} />
      <label>Téléphone</label><input value={f.telephone} onChange={maj('telephone')} />
      <label>Mot de passe</label><input type="password" minLength={8} required value={f.motDePasse} onChange={maj('motDePasse')} />
      <button type="submit">Créer</button>{msg && <p className={msg === 'Compte créé.' ? 'ok' : 'err'}>{msg}</p>}</form>
  );
}

export default function Admin() {
  const { user } = useAuth();
  const estAdmin = user.role === 'ADMIN';
  const onglets = estAdmin
    ? [['validation', 'Validation'], ['moderation', 'Modération'], ['dashboard', 'Tableau de bord'], ['publication', 'Alertes et campagnes'], ['users', 'Comptes']]
    : [['moderation', 'Modération']];
  const [tab, setTab] = useState(onglets[0][0]);
  return (
    <>
      <div className="onglets">
        {onglets.map(([k, l]) => <button key={k} className={tab === k ? 'actif' : ''} onClick={() => setTab(k)}>{l}</button>)}
      </div>
      {tab === 'validation' && <Validation />}
      {tab === 'moderation' && <Moderation />}
      {tab === 'dashboard' && <Dashboard />}
      {tab === 'publication' && <Publication />}
      {tab === 'users' && <Utilisateurs />}
    </>
  );
}
