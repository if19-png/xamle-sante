import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { msgErreur } from '../api';
import { useAuth } from '../auth';

export default function Inscription() {
  const [f, setF] = useState({ email: '', motDePasse: '', nom: '', prenom: '', telephone: '', role: 'PATIENT' });
  const [erreur, setErreur] = useState('');
  const { connecter } = useAuth();
  const navigate = useNavigate();
  const maj = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const envoyer = async (e) => {
    e.preventDefault();
    setErreur('');
    try {
      const { data } = await api.post('/api/auth/register', f);
      connecter(data.token);
      navigate(f.role === 'PROFESSIONNEL' ? '/pro' : '/patient');
    } catch (err) {
      setErreur(msgErreur(err));
    }
  };

  return (
    <form className="carte" onSubmit={envoyer}>
      <h1>Inscription</h1>
      <label>Je suis</label>
      <select value={f.role} onChange={maj('role')}>
        <option value="PATIENT">Patient / citoyen</option>
        <option value="PROFESSIONNEL">Professionnel de santé</option>
      </select>
      <div className="grille">
        <div><label>Prénom</label><input value={f.prenom} onChange={maj('prenom')} required /></div>
        <div><label>Nom</label><input value={f.nom} onChange={maj('nom')} required /></div>
      </div>
      <label>E-mail</label>
      <input type="email" value={f.email} onChange={maj('email')} required />
      <label>Téléphone (pour les rappels SMS)</label>
      <input value={f.telephone} onChange={maj('telephone')} placeholder="77 123 45 67" />
      <label>Mot de passe (8 caractères minimum)</label>
      <input type="password" value={f.motDePasse} onChange={maj('motDePasse')} minLength={8} required />
      {erreur && <p className="err">{erreur}</p>}
      <button type="submit">Créer mon compte</button>
    </form>
  );
}
