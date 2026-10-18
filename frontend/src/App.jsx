import { Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { useAuth } from './auth';
import { Cross } from './icons';
import Flottants from './Flottants';
import Accueil from './pages/Accueil';
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
      <Flottants />
      <header className="entete">
        <div className="entete-interne">
          <Link to="/" className="marque">
            <span className="logo-croix"><Cross width={16} height={16} /></span>
            <span>Xamlé <b>Santé</b></span>
          </Link>
          <nav>
            <NavLink to="/" end>Accueil</NavLink>
            <NavLink to="/annuaire">Annuaire</NavLink>
            <NavLink to="/epidemio">Alertes santé</NavLink>
            <NavLink to="/campagnes">Campagnes</NavLink>
            {user?.role === 'PATIENT' && <NavLink to="/patient">Mes RDV</NavLink>}
            {user?.role === 'PROFESSIONNEL' && <NavLink to="/pro">Espace pro</NavLink>}
            {(user?.role === 'ADMIN' || user?.role === 'MODERATEUR') && <NavLink to="/admin">Administration</NavLink>}
            {user ? (
              <button className="lien" onClick={() => { deconnecter(); navigate('/'); }}>Déconnexion</button>
            ) : (
              <>
                <NavLink to="/connexion">Connexion</NavLink>
                <NavLink to="/inscription" className="lien-inscription">Inscription</NavLink>
              </>
            )}
            <NavLink to="/sos" className="lien-sos">SOS</NavLink>
          </nav>
        </div>
      </header>

      <main className="page">
        <Routes>
          <Route path="/" element={<Accueil />} />
          <Route path="/annuaire" element={<Recherche />} />
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

      <footer className="pied">
        <div className="pied-interne">
          <span className="marque-pied"><Cross width={14} height={14} /> Xamlé Santé</span>
          <span>Annuaire de santé de Ziguinchor · Projet UASZ, Licence 3 Ingénierie Informatique, 2025-2026</span>
          <span>En cas d'urgence vitale, utilisez le bouton SOS ou contactez directement les services d'urgence.</span>
        </div>
      </footer>
    </>
  );
}
