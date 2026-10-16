import { NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { useAuth } from './auth';
import Recherche from './pages/Recherche';
import Sos from './pages/Sos';
import Epidemio from './pages/Epidemio';
import Campagnes from './pages/Campagnes';
import Connexion from './pages/Connexion';
import Inscription from './pages/Inscription';
import EspacePro from './pages/EspacePro';
import EspacePatient from './pages/EspacePatient';
import Admin from './pages/Admin';

function Prive({ roles, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/connexion" />;
  if (roles && !roles.includes(user.role)) return <p className="carte">Accès refusé pour ce rôle.</p>;
  return children;
}

export default function App() {
  const { user, deconnecter } = useAuth();
  const navigate = useNavigate();
  return (
    <>
      <header className="entete">
        <strong className="logo">Xamlé Santé</strong>
        <nav>
          <NavLink to="/" end>Annuaire</NavLink>
          <NavLink to="/sos" className="lien-sos">SOS</NavLink>
          <NavLink to="/epidemio">Alertes santé</NavLink>
          <NavLink to="/campagnes">Campagnes</NavLink>
          {user?.role === 'PATIENT' && <NavLink to="/patient">Mes RDV</NavLink>}
          {user?.role === 'PROFESSIONNEL' && <NavLink to="/pro">Espace pro</NavLink>}
          {(user?.role === 'ADMIN' || user?.role === 'MODERATEUR') && <NavLink to="/admin">Administration</NavLink>}
          {user ? (
            <button className="lien" onClick={() => { deconnecter(); navigate('/'); }}>
              Déconnexion ({user.role})
            </button>
          ) : (
            <>
              <NavLink to="/connexion">Connexion</NavLink>
              <NavLink to="/inscription">Inscription</NavLink>
            </>
          )}
        </nav>
      </header>
      <main className="page">
        <Routes>
          <Route path="/" element={<Recherche />} />
          <Route path="/sos" element={<Sos />} />
          <Route path="/epidemio" element={<Epidemio />} />
          <Route path="/campagnes" element={<Campagnes />} />
          <Route path="/connexion" element={<Connexion />} />
          <Route path="/inscription" element={<Inscription />} />
          <Route path="/patient" element={<Prive roles={['PATIENT']}><EspacePatient /></Prive>} />
          <Route path="/pro" element={<Prive roles={['PROFESSIONNEL']}><EspacePro /></Prive>} />
          <Route path="/admin" element={<Prive roles={['ADMIN', 'MODERATEUR']}><Admin /></Prive>} />
          <Route path="*" element={<p className="carte">Page introuvable.</p>} />
        </Routes>
      </main>
    </>
  );
}
