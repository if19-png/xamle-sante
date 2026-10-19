# Démonstration de Xamlé Santé (5 min) et questions probables

## Avant de commencer (5 minutes avant)

- Ouvrir https://xamle-sante-uasz.vercel.app **et** https://xamle-sante.onrender.com/swagger-ui/index.html : le serveur gratuit dort, le premier chargement prend jusqu'à deux minutes.
- Se connecter une fois en `patient@xamle.sn`, `pro4@xamle.sn`, `admin@xamle.sn`, `moderateur@xamle.sn` (mot de passe `Xamle2026!`) pour vérifier que tout répond. Utiliser plusieurs fenêtres ou un navigateur différent par rôle.
- Autoriser la localisation dans le navigateur. Sans GPS, le SOS propose une position approximative (centre de Ziguinchor).
- Garder le terminal ou le tableau de bord Render avec les logs ouverts : on y voit le message du SMS simulé.

## Déroulé

| Temps | Étape | À dire |
|---|---|---|
| 0:00 | **Accueil** | Le problème : en Casamance, trouver un spécialiste ou une urgence est difficile et l'information est dispersée. Xamlé Santé centralise tout. |
| 0:30 | **Annuaire** : chercher une spécialité, montrer le tri par distance et la carte. Ouvrir « Horaires » et « Avis » sur une fiche. | Calcul de distance côté base avec PostGIS. Seuls les profils validés sont publics. |
| 1:15 | **Établissements** : filtrer « Pharmacies », puis « Ouvert 24h/24 ». Cliquer une carte, la carte s'envole vers le marqueur. | Services, horaires, téléphone, itinéraire. |
| 1:45 | **SOS** : saisir un numéro, appuyer. Montrer le bandeau et le message. | Position GPS, urgences les plus proches, message avec lien de carte. L'envoi SMS est **simulé** et le dit à l'écran ; Twilio est codé et activable par une variable. |
| 2:30 | **Validation** : connexion `pro4@xamle.sn` (profil en attente), puis `admin@xamle.sn` : valider ; retrouver le profil dans l'annuaire. | Workflow BROUILLON, EN_ATTENTE, VALIDE ou REJETE. |
| 3:15 | **Carte de chaleur** : Boucotte et Lyndiane visibles, Kandé absent. | Seuil d'anonymat de 3 signalements validés : en dessous, le quartier est masqué. Le signalement n'a **aucune colonne vers l'utilisateur**, même la base ne permet pas de l'identifier. |
| 3:45 | **Patient** : donner un avis, s'abonner à une campagne, prendre un rendez-vous en visio, payer (Wave simulé), montrer le lien de visio. | Frais de dossier de 1 000 FCFA, rappel SMS 24 h avant par une tâche programmée. |
| 4:30 | **Admin** : tableau de bord, zones à risque, export CSV. | Données agrégées, utiles à la santé publique. |
| 4:50 | **Conclusion** | Swagger, 66 tests automatiques, déploiement en ligne, UML dans `docs/`. |

## Si quelque chose ne marche pas pendant la démo

- **Page lente ou « serveur injoignable »** : attendre, le serveur se réveille. Recharger après une minute.
- **Erreur 401 sur une page publique** : un ancien jeton est resté dans le navigateur. F12, Application, Local Storage, supprimer `token`, recharger.
- **Position introuvable au SOS** : utiliser le bouton « position approximative » et le dire.

## Questions probables

**Le SMS part-il vraiment ?** Non, il est simulé : le message est généré, enregistré dans `sos_log` et écrit dans les logs. L'écran l'indique. Twilio est codé (`SMS_MODE=twilio`). En compte d'essai il n'envoie qu'à des numéros vérifiés, et vers le Sénégal Orange et Expresso demandent un identifiant d'expéditeur enregistré.

**Wave est-il réel ?** Non, c'est un paiement simulé (référence générée localement, frais de 1 000 FCFA). En production, seul `RendezVousService.payer` est à brancher sur l'API d'un fournisseur.

**Comment garantissez-vous l'anonymat ?** Par la structure de la base (aucun lien signalement-utilisateur), par la modération avant comptage, et par le seuil de 3 signalements avant affichage d'un quartier.

**Pourquoi PostGIS ?** Pour trier et filtrer par distance directement en SQL, avec un index spatial.

**Comment sont gérés les rôles ?** JWT signé, rôle dans le jeton, routes protégées par rôle (`/api/admin`, `/api/pro`, `/api/patient`, `/api/moderation`). Les tests vérifient les refus (403) et l'impossibilité de s'inscrire en administrateur.

**Qu'est-ce qui n'est pas fini ?** Les SMS et les paiements réels, les notifications push, l'envoi de photos par fichier, le rate limiting, et des données réelles pour les établissements (les numéros affichés sont fictifs).

**Quelle couverture de tests ?** Voir `backend/target/site/jacoco/index.html` ; 66 tests dont 33 d'intégration sur une vraie base PostGIS.
