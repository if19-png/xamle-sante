import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import Flottants from '../Flottants';
import { Alerte, Bouclier, Calendrier, Eclair, Loupe, Pin, Pouls, Telephone } from '../icons';

const ACTIONS = [
  { to: '/annuaire', Icone: Loupe, titre: 'Trouver un spécialiste', texte: 'Par spécialité, langue et distance, avec la carte.' },
  { to: '/sos', Icone: Telephone, titre: 'Urgence SOS', texte: 'Envoyez votre position par SMS et voyez les urgences les plus proches.', urgence: true },
  { to: '/epidemio', Icone: Pouls, titre: 'Alertes santé', texte: 'Suivez la situation par quartier et signalez des symptômes, en restant anonyme.' },
  { to: '/campagnes', Icone: Calendrier, titre: 'Campagnes', texte: 'Vaccination, dépistage, sensibilisation : recevez un rappel par SMS.' },
];

const ATOUTS = [
  { Icone: Bouclier, titre: 'Profils vérifiés', texte: 'Chaque spécialiste est validé par l\'administration avant d\'apparaître dans l\'annuaire.' },
  { Icone: Eclair, titre: 'Rapide en urgence', texte: 'Un seul bouton localise votre position et prévient un proche par SMS.' },
  { Icone: Alerte, titre: 'Anonyme et protégé', texte: 'Les signalements ne sont reliés à aucune identité et ne s\'affichent qu\'agrégés par quartier.' },
];

const ETAPES = [
  ['1', 'Cherchez', 'Choisissez une spécialité, une langue, un rayon autour de vous.'],
  ['2', 'Comparez', 'Consultez les fiches et la distance sur la carte.'],
  ['3', 'Consultez', 'Prenez rendez-vous en présentiel ou en visio.'],
];

export default function Accueil() {
  const [chiffres, setChiffres] = useState(null);

  useEffect(() => {
    Promise.all([api.get('/api/public/professionnels'), api.get('/api/public/etablissements')])
      .then(([p, e]) => setChiffres({ pros: p.data.length, etabs: e.data.length }))
      .catch(() => {});
  }, []);

  return (
    <>
      <section className="hero">
        <div className="hero-contenu">
          <span className="pastille"><Pin width={14} height={14} /> Ziguinchor · Casamance</span>
          <h1>Votre santé,<br />à portée de main</h1>
          <p>Trouvez le bon spécialiste, localisez les urgences les plus proches et suivez les alertes de santé publique, depuis votre téléphone.</p>
          <div className="hero-boutons">
            <Link className="bouton clair" to="/annuaire">Trouver un spécialiste</Link>
            <Link className="bouton urgence" to="/sos">SOS Urgence</Link>
          </div>
        </div>
        <Flottants variante="hero" />
        <svg className="hero-pouls" viewBox="0 0 600 120" preserveAspectRatio="none" aria-hidden="true">
          <polyline className="ligne-pouls"
            points="0,60 120,60 150,60 170,22 195,100 220,12 245,60 330,60 360,60 380,32 400,86 420,60 600,60" />
        </svg>
      </section>

      {chiffres && (
        <section className="chiffres">
          <div><b>{chiffres.pros}</b><span>spécialistes vérifiés</span></div>
          <div><b>{chiffres.etabs}</b><span>établissements référencés</span></div>
          <div><b>24 h</b><span>urgences accessibles</span></div>
        </section>
      )}

      <h2 className="titre-section">Que cherchez-vous ?</h2>
      <section className="tuiles">
        {ACTIONS.map(({ to, Icone, titre, texte, urgence }) => (
          <Link key={to} to={to} className={`tuile ${urgence ? 'urgence' : ''}`}>
            <span className="tuile-icone"><Icone width={26} height={26} /></span>
            <h3>{titre}</h3>
            <p>{texte}</p>
          </Link>
        ))}
      </section>

      <section className="bandeau-photo">
        <img src="/images/equipe.jpg" alt="Équipe de professionnels de santé"
          onError={(e) => { e.currentTarget.style.display = 'none'; }} />
        <div>
          <h2>Une santé plus proche de chacun</h2>
          <p>En Casamance, l'information médicale circule surtout de bouche à oreille. Xamlé Santé rassemble au même endroit les spécialistes, les établissements, les urgences et les campagnes de prévention.</p>
          <Link className="bouton" to="/annuaire">Découvrir l'annuaire</Link>
        </div>
      </section>

      <h2 className="titre-section">Pourquoi Xamlé Santé ?</h2>
      <section className="atouts">
        {ATOUTS.map(({ Icone, titre, texte }) => (
          <div className="atout" key={titre}>
            <span className="atout-icone"><Icone width={22} height={22} /></span>
            <h3>{titre}</h3>
            <p>{texte}</p>
          </div>
        ))}
      </section>

      <h2 className="titre-section">Comment ça marche</h2>
      <section className="etapes">
        {ETAPES.map(([n, t, d]) => (
          <div className="etape" key={n}><span className="etape-num">{n}</span><h3>{t}</h3><p>{d}</p></div>
        ))}
      </section>

      <section className="appel-pro">
        <div>
          <h2>Vous êtes professionnel de santé ?</h2>
          <p>Créez votre profil, faites-le valider et soyez trouvé par les patients de votre ville.</p>
        </div>
        <Link className="bouton clair" to="/inscription">Rejoindre l'annuaire</Link>
      </section>
    </>
  );
}
