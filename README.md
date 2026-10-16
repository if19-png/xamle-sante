# Xamlé Santé

Annuaire intelligent des spécialistes et urgences médicales de Ziguinchor.
Projet 13, Licence 3 Ingénierie Informatique, UASZ, 2025-2026.

**Stack** : Spring Boot 4 · PostgreSQL 16 + PostGIS · React (Vite, PWA) · Leaflet · JWT · Swagger.

## Fonctionnalités

- **Annuaire géolocalisé** : recherche par spécialité, langue, texte, rayon, disponibilité ; résultats **triés par distance** (PostGIS) et affichés sur carte Leaflet.
- **Workflow de validation** des profils : BROUILLON → EN_ATTENTE → VALIDE / REJETE (seuls les profils VALIDES sont publics).
- **SOS** : position GPS, urgences les plus proches, SMS (Twilio ou simulation).
- **Épidémiologie** : signalements anonymes (aucun lien vers un utilisateur), modération médicale, carte de chaleur agrégée par quartier avec seuil d'anonymat (3), alertes.
- **Agenda santé** : campagnes, abonnement, rappel SMS 24 h avant (tâche `@Scheduled`).
- **Rendez-vous** et paiement des frais de dossier (1 000 FCFA) via un Wave **simulé** pour la consultation vidéo.
- **Tableau de bord** santé publique, zones à risque, export CSV anonymisé.
- **Sécurité** : JWT, 4 rôles (ADMIN, PROFESSIONNEL, PATIENT, MODERATEUR), mots de passe BCrypt.
- **PWA** : mobile-first, Service Worker (mode hors ligne partiel).

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

1. Annuaire : recherche par spécialité, tri par distance, carte.
2. SOS : saisir un numéro, appuyer, montrer le SMS (log `[SMS SIMULÉ]` si simulation).
3. Connexion `pro4@xamle.sn` : profil EN_ATTENTE. Connexion `admin@xamle.sn` : valider, puis retrouver le profil dans l'annuaire.
4. Connexion `moderateur@xamle.sn` : valider un signalement. Carte de chaleur : Boucotte et Lyndiane visibles, Kandé masqué (2 signalements, sous le seuil).
5. Connexion `patient@xamle.sn` : s'abonner à la campagne de moins de 24 h ; admin : « Envoyer les rappels dus » ; SMS dans les logs.
6. Admin : tableau de bord, zones à risque, export CSV.

## Limites connues

- Les coordonnées des quartiers et les établissements marqués « (démo) » sont **fictifs ou approximatifs** : à remplacer par des données réelles.
- Wave est **simulé** (aucune API externe appelée).
- Pas de limitation de débit (rate limiting) sur les endpoints publics (signalements, SOS).
- Couverture de tests partielle : voir `backend/src/test`.

## Documentation

- UML (PlantUML) dans `docs/` : cas d'utilisation, classes, séquences SOS et validation. À afficher avec l'extension PlantUML de VS Code ou sur plantuml.com.
- Swagger / OpenAPI : `/swagger-ui/index.html` et `/v3/api-docs`.

## Branches Git

`main` (stable), `develop` (intégration), `feature/*` (une branche par fonctionnalité).
