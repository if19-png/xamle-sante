# Xamlé Santé

Annuaire intelligent des spécialistes et urgences médicales de Ziguinchor.
Projet 13, Licence 3 Ingénierie Informatique, UASZ, 2025-2026.

**Stack** : Spring Boot 4 · PostgreSQL 16 + PostGIS · React (Vite, PWA) · Leaflet · JWT · Swagger.

## Fonctionnalités

- **Annuaire géolocalisé** : recherche par spécialité, langue, texte, rayon, disponibilité ; résultats **triés par distance** (PostGIS) et affichés sur carte Leaflet.
- **Workflow de validation** des profils : BROUILLON → EN_ATTENTE → VALIDE / REJETE (seuls les profils VALIDES sont publics).
- **Établissements** : hôpitaux, cliniques, dispensaires et pharmacies sur carte, avec services, horaires, téléphone, filtre « ouvert 24h/24 » et itinéraire.
- **Fiches de spécialistes** : photo (par lien), horaires saisis par le professionnel, avis et notes des patients (1 à 5).
- **SOS** : position GPS, urgences les plus proches, message de position par SMS. L'envoi est **simulé** tant que Twilio n'est pas activé, et l'écran l'indique clairement (bandeau orange).
- **Épidémiologie** : signalements anonymes (aucun lien vers un utilisateur), modération médicale, carte de chaleur agrégée par quartier avec seuil d'anonymat (3), alertes.
- **Agenda santé** : campagnes, abonnement, rappel SMS 24 h avant (tâche `@Scheduled`).
- **Rendez-vous** et paiement des frais de dossier (1 000 FCFA) via un Wave **simulé** pour la consultation vidéo.
- **Tableau de bord** santé publique, zones à risque, export CSV anonymisé.
- **Sécurité** : JWT, 4 rôles (ADMIN, PROFESSIONNEL, PATIENT, MODERATEUR), mots de passe BCrypt.
- **PWA** : mobile-first, Service Worker (mode hors ligne partiel).

## Application en ligne

| Élément | Adresse |
|---|---|
| Site (Vercel) | https://xamle-sante-uasz.vercel.app |
| API (Render) | https://xamle-sante.onrender.com |
| Swagger | https://xamle-sante.onrender.com/swagger-ui/index.html |
| Base de données | Neon (PostgreSQL + PostGIS) |

L'offre gratuite de Render met le serveur en veille : la première requête peut prendre une à deux minutes. Ouvrir le site quelques minutes avant une présentation.

Variables définies sur Render : `DB_HOST`, `DB_PORT`, `DB_NAME` (avec `?sslmode=require`), `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `SEED_DEMO=true` (comptes de démonstration), `CORS_ORIGINS` (adresse du site), `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` et `SPRING_FLYWAY_BASELINE_VERSION=0` (le schéma `public` de Neon contient déjà les tables PostGIS). Sur Vercel : `VITE_API_URL`.

## Prérequis

JDK 21, Node 20+, PostgreSQL 16 avec PostGIS, Git.

## Lancer en local

### 1. Base de données

```sql
CREATE DATABASE xamle_sante;
\c xamle_sante
CREATE EXTENSION postgis;
```

Flyway crée les tables (V1) et les données de départ (V2) au premier démarrage.

### 2. Backend

Variables d'environnement :

| Variable | Obligatoire | Rôle |
|---|---|---|
| `DB_PASSWORD` | oui | Mot de passe de l'utilisateur PostgreSQL |
| `JWT_SECRET` | oui | Secret de signature JWT, 32 caractères minimum |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` | non | Par défaut `localhost`, `5432`, `xamle_sante`, `postgres` |
| `SEED_DEMO` | non | `true` (défaut) crée les comptes et données de démonstration. **Mettre `false` en production.** |
| `CORS_ORIGINS` | non | Origines autorisées, par défaut `http://localhost:5173` |
| `SMS_MODE` | non | `simulation` (défaut, SMS journalisés) ou `twilio` |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM` | si `twilio` | Identifiants Twilio |

```bash
cd backend
./mvnw spring-boot:run        # Windows : mvnw.cmd spring-boot:run
```

API : http://localhost:8080 · Swagger : http://localhost:8080/swagger-ui/index.html

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Application : http://localhost:5173 (l'URL de l'API se règle avec `VITE_API_URL`, voir `.env.example`).

## Comptes de démonstration

Mot de passe de tous les comptes : `Xamle2026!`

| E-mail | Rôle |
|---|---|
| admin@xamle.sn | ADMIN |
| moderateur@xamle.sn | MODERATEUR |
| patient@xamle.sn | PATIENT |
| pro1@xamle.sn à pro3@xamle.sn | PROFESSIONNEL (profils validés) |
| pro4@xamle.sn | PROFESSIONNEL (profil EN_ATTENTE, pour démontrer la validation) |

## Anonymisation (critère du brief)

- La table `signalement` n'a aucune colonne vers `utilisateur` : même avec un accès à la base, aucun signalement n'est rattachable à une personne.
- La carte de chaleur et l'export CSV n'affichent un quartier qu'à partir de `app.epidemio.seuil-anonymat` signalements validés (3).
- Les signalements sont soumis à modération avant de compter.

## Scénario de démonstration (5 min)

Le déroulé détaillé, avec le temps de chaque étape et les réponses aux questions probables, est dans `docs/SCENARIO_DEMO.md`. En résumé :

1. Annuaire : recherche par spécialité, tri par distance, carte ; ouvrir une fiche (photo, horaires, avis).
2. Établissements : carte, filtres, services, horaires, itinéraire.
3. SOS : saisir un numéro, appuyer ; le bandeau précise que le SMS est simulé.
4. Connexion `pro4@xamle.sn` : profil EN_ATTENTE. Connexion `admin@xamle.sn` : valider, puis retrouver le profil dans l'annuaire.
5. Connexion `moderateur@xamle.sn` : modération. Carte de chaleur : Boucotte et Lyndiane visibles, Kandé masqué (2 signalements, sous le seuil d'anonymat).
6. Connexion `patient@xamle.sn` : donner un avis, s'abonner à une campagne, prendre un rendez-vous en visio (paiement Wave simulé, lien de visioconférence).
7. Admin : tableau de bord, zones à risque, export CSV.

## Tests

```bash
cd backend
./mvnw clean test        # Windows PowerShell : .\mvnw clean test
```

- **Tests unitaires** (sans base) : validation des données, gestionnaire d'erreurs, jetons JWT, SMS, règles des horaires et des avis.
- **Tests d'intégration** (`ApiIntegrationTest`, `ApiScenariosTest`) : l'application démarre pour de vrai et l'API est appelée (sécurité par rôles, SOS, anonymat de la carte de chaleur, rendez-vous et paiement simulé, campagnes, validation et modération). Ils s'exécutent seulement si `DB_PASSWORD` est défini et utilisent une base **dédiée** (`DB_TEST_NAME`, par défaut `xamle_sante_test`), jamais la base de développement ni celle du déploiement. Pour une base Neon, mettre `?sslmode=require` à la suite du nom.
- **Couverture** (JaCoCo) : rapport dans `backend/target/site/jacoco/index.html` après `mvnw clean test`.

## Limites connues

- Les coordonnées des quartiers et les établissements marqués « (démo) » sont **fictifs ou approximatifs**. Les numéros `33 000 00 0X` des établissements sont **fictifs** : à remplacer par les vrais.
- **L'envoi de SMS est simulé** par défaut (message généré et journalisé, aucun SMS réel). Twilio est codé et s'active avec `SMS_MODE=twilio` ; en compte d'essai il n'envoie qu'à des numéros vérifiés, et vers le Sénégal les opérateurs Orange et Expresso exigent un identifiant d'expéditeur alphanumérique enregistré.
- **Wave est simulé** (aucune API externe appelée).
- Pas de notification push : seuls le SMS (simulé) et les notifications internes existent.
- Photos des spécialistes : par lien vers une image, pas d'envoi de fichier.
- L'éditeur d'horaires gère une seule plage par jour.
- Pas de limitation de débit (rate limiting) sur les endpoints publics (signalements, SOS).
- Offre gratuite de Render : mise en veille du serveur après inactivité.

## Documentation

- UML (PlantUML) dans `docs/` : cas d'utilisation, classes, séquences SOS et validation ; images dans `docs/img/`.
- Swagger / OpenAPI : `/swagger-ui/index.html` et `/v3/api-docs` ; export dans `docs/openapi.json`.
- Déroulé de la démonstration : `docs/SCENARIO_DEMO.md`.

## Branches Git

`main` (stable), `develop` (intégration), `feature/*` (une branche par fonctionnalité).
