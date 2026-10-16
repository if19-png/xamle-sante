import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { msgErreur } from '../api';
import { useAuth } from '../auth';

export default function Connexion() {
  const [email, setEmail] = useState('');
  const [motDePasse, setMdp] = useState('');
  const [erreur, setErreur] = useState('');
  const { connecter } = useAuth();
  const navigate = useNavigate();

  const envoyer = async (e) => {
    e.preventDefault();
    setErreur('');
    try {
      const { data } = await api.post('/api/auth/login', { email, motDePasse });
      connecter(data.token);
      navigate(data.role === 'ADMIN' || data.role === 'MODERATEUR' ? '/admin'
        : data.role === 'PROFESSIONNEL' ? '/pro' : data.role === 'PATIENT' ? '/patient' : '/');
    } catch (err) {
      setErreur(msgErreur(err));
    }
  };

  return (
    <form className="carte" onSubmit={envoyer}>
      <h1>Connexion</h1>
      <label>E-mail</label>
      <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      <label>Mot de passe</label>
      <input type="password" value={motDePasse} onChange={(e) => setMdp(e.target.value)} required />
      {erreur && <p className="err">{erreur}</p>}
      <button type="submit">Se connecter</button>
    </form>
  );
}
