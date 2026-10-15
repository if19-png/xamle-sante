CREATE EXTENSION IF NOT EXISTS postgis;
 
-- ========== UTILISATEURS ==========
CREATE TABLE utilisateur (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe  VARCHAR(255) NOT NULL,
    nom           VARCHAR(100) NOT NULL,
    prenom        VARCHAR(100) NOT NULL,
    telephone     VARCHAR(20),
    role          VARCHAR(20) NOT NULL
        CHECK (role IN ('ADMIN','PROFESSIONNEL','PATIENT','MODERATEUR')),
    actif         BOOLEAN NOT NULL DEFAULT TRUE,
    cree_le       TIMESTAMP NOT NULL DEFAULT NOW()
);
 
-- ========== ANNUAIRE ==========
CREATE TABLE specialite (
    id   SERIAL PRIMARY KEY,
    nom  VARCHAR(100) NOT NULL UNIQUE
);
 
CREATE TABLE langue (
    id   SERIAL PRIMARY KEY,
    nom  VARCHAR(50) NOT NULL UNIQUE
);
 
CREATE TABLE etablissement (
    id          BIGSERIAL PRIMARY KEY,
    nom         VARCHAR(200) NOT NULL,
    type        VARCHAR(20) NOT NULL
        CHECK (type IN ('HOPITAL','CLINIQUE','PHARMACIE','DISPENSAIRE')),
    adresse     VARCHAR(255),
    telephone   VARCHAR(20),
    services    TEXT,
    ouvert_24h  BOOLEAN NOT NULL DEFAULT FALSE,
    position    geography(Point, 4326) NOT NULL
);
CREATE INDEX idx_etablissement_position ON etablissement USING GIST (position);
 
CREATE TABLE professionnel (
    id                 BIGSERIAL PRIMARY KEY,
    utilisateur_id     BIGINT NOT NULL UNIQUE REFERENCES utilisateur(id),
    specialite_id      INT NOT NULL REFERENCES specialite(id),
    etablissement_id   BIGINT REFERENCES etablissement(id),
    photo_url          VARCHAR(255),
    telephone_pro      VARCHAR(20),
    bio                TEXT,
    position           geography(Point, 4326),
    statut_validation  VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
        CHECK (statut_validation IN ('BROUILLON','EN_ATTENTE','VALIDE','REJETE')),
    motif_rejet        TEXT,
    valide_par         BIGINT REFERENCES utilisateur(id),
    valide_le          TIMESTAMP,
    consultation_video BOOLEAN NOT NULL DEFAULT FALSE,
    cree_le            TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_professionnel_position ON professionnel USING GIST (position);
CREATE INDEX idx_professionnel_statut ON professionnel (statut_validation);
 
CREATE TABLE professionnel_langue (
    professionnel_id BIGINT REFERENCES professionnel(id) ON DELETE CASCADE,
    langue_id        INT    REFERENCES langue(id),
    PRIMARY KEY (professionnel_id, langue_id)
);
 
CREATE TABLE horaire (
    id               BIGSERIAL PRIMARY KEY,
    professionnel_id BIGINT NOT NULL REFERENCES professionnel(id) ON DELETE CASCADE,
    jour_semaine     SMALLINT NOT NULL CHECK (jour_semaine BETWEEN 1 AND 7),
    heure_debut      TIME NOT NULL,
    heure_fin        TIME NOT NULL,
    CHECK (heure_fin > heure_debut)
);
 
CREATE TABLE avis (
    id               BIGSERIAL PRIMARY KEY,
    professionnel_id BIGINT NOT NULL REFERENCES professionnel(id) ON DELETE CASCADE,
    patient_id       BIGINT NOT NULL REFERENCES utilisateur(id),
    note             SMALLINT NOT NULL CHECK (note BETWEEN 1 AND 5),
    commentaire      TEXT,
    cree_le          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (professionnel_id, patient_id)
);
 
-- ========== RENDEZ-VOUS & PAIEMENT ==========
CREATE TABLE rendez_vous (
    id               BIGSERIAL PRIMARY KEY,
    patient_id       BIGINT NOT NULL REFERENCES utilisateur(id),
    professionnel_id BIGINT NOT NULL REFERENCES professionnel(id),
    date_heure       TIMESTAMP NOT NULL,
    type             VARCHAR(15) NOT NULL CHECK (type IN ('PRESENTIEL','VIDEO')),
    statut           VARCHAR(15) NOT NULL DEFAULT 'DEMANDE'
        CHECK (statut IN ('DEMANDE','CONFIRME','ANNULE','TERMINE')),
    lien_video       VARCHAR(255),
    motif            TEXT,
    cree_le          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_rdv_pro_date ON rendez_vous (professionnel_id, date_heure);
 
CREATE TABLE paiement (
    id               BIGSERIAL PRIMARY KEY,
    rendez_vous_id   BIGINT NOT NULL UNIQUE REFERENCES rendez_vous(id),
    montant          INT NOT NULL DEFAULT 1000,
    devise           VARCHAR(3) NOT NULL DEFAULT 'XOF',
    statut           VARCHAR(15) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (statut IN ('EN_ATTENTE','PAYE','ECHOUE')),
    reference_wave   VARCHAR(100),
    cree_le          TIMESTAMP NOT NULL DEFAULT NOW(),
    maj_le           TIMESTAMP
);
 
-- ========== URGENCES ==========
CREATE TABLE service_urgence (
    id               BIGSERIAL PRIMARY KEY,
    etablissement_id BIGINT NOT NULL REFERENCES etablissement(id),
    type             VARCHAR(30) NOT NULL,
    telephone        VARCHAR(20),
    disponible_24h   BOOLEAN NOT NULL DEFAULT TRUE
);
 
CREATE TABLE sos_log (
    id             BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT REFERENCES utilisateur(id),
    position       geography(Point, 4326) NOT NULL,
    telephone_dest VARCHAR(20),
    sms_envoye     BOOLEAN NOT NULL DEFAULT FALSE,
    erreur_sms     TEXT,
    cree_le        TIMESTAMP NOT NULL DEFAULT NOW()
);
 
-- ========== EPIDEMIOLOGIE ==========
CREATE TABLE quartier (
    id        SERIAL PRIMARY KEY,
    nom       VARCHAR(100) NOT NULL UNIQUE,
    commune   VARCHAR(100) NOT NULL DEFAULT 'Ziguinchor',
    zone      geography(MultiPolygon, 4326) NOT NULL
);
CREATE INDEX idx_quartier_zone ON quartier USING GIST (zone);
 
-- ATTENTION : volontairement AUCUNE colonne utilisateur_id (anonymat)
CREATE TABLE signalement (
    id           BIGSERIAL PRIMARY KEY,
    quartier_id  INT NOT NULL REFERENCES quartier(id),
    symptome     VARCHAR(30) NOT NULL
        CHECK (symptome IN ('FIEVRE','DIARRHEE','TOUX','VOMISSEMENTS','ERUPTION','AUTRE')),
    description  TEXT,
    statut       VARCHAR(15) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (statut IN ('EN_ATTENTE','VALIDE','REJETE')),
    modere_par   BIGINT REFERENCES utilisateur(id),
    modere_le    TIMESTAMP,
    cree_le      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_signalement_agg ON signalement (quartier_id, symptome, statut, cree_le);
 
CREATE TABLE alerte (
    id          BIGSERIAL PRIMARY KEY,
    quartier_id INT REFERENCES quartier(id),
    niveau      VARCHAR(10) NOT NULL CHECK (niveau IN ('INFO','VIGILANCE','ALERTE')),
    titre       VARCHAR(150) NOT NULL,
    message     TEXT NOT NULL,
    cree_par    BIGINT NOT NULL REFERENCES utilisateur(id),
    cree_le     TIMESTAMP NOT NULL DEFAULT NOW(),
    expire_le   TIMESTAMP
);
 
-- ========== AGENDA SANTE ==========
CREATE TABLE campagne (
    id           BIGSERIAL PRIMARY KEY,
    type         VARCHAR(20) NOT NULL
        CHECK (type IN ('VACCINATION','DEPISTAGE','SENSIBILISATION')),
    titre        VARCHAR(150) NOT NULL,
    description  TEXT,
    lieu         VARCHAR(200),
    position     geography(Point, 4326),
    date_debut   TIMESTAMP NOT NULL,
    date_fin     TIMESTAMP,
    cree_par     BIGINT NOT NULL REFERENCES utilisateur(id)
);
 
CREATE TABLE abonnement_campagne (
    id             BIGSERIAL PRIMARY KEY,
    campagne_id    BIGINT NOT NULL REFERENCES campagne(id) ON DELETE CASCADE,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id),
    UNIQUE (campagne_id, utilisateur_id)
);
 
CREATE TABLE rappel_sms (
    id             BIGSERIAL PRIMARY KEY,
    campagne_id    BIGINT NOT NULL REFERENCES campagne(id) ON DELETE CASCADE,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id),
    envoi_prevu_le TIMESTAMP NOT NULL,
    statut         VARCHAR(10) NOT NULL DEFAULT 'PLANIFIE'
        CHECK (statut IN ('PLANIFIE','ENVOYE','ECHEC')),
    envoye_le      TIMESTAMP,
    UNIQUE (campagne_id, utilisateur_id)
);
CREATE INDEX idx_rappel_a_envoyer ON rappel_sms (statut, envoi_prevu_le);
 
-- ========== TRANSVERSAL ==========
CREATE TABLE notification (
    id             BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    titre          VARCHAR(150) NOT NULL,
    contenu        TEXT,
    lue            BOOLEAN NOT NULL DEFAULT FALSE,
    cree_le        TIMESTAMP NOT NULL DEFAULT NOW()
);
 
CREATE TABLE journal_audit (
    id             BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT REFERENCES utilisateur(id),
    action         VARCHAR(80) NOT NULL,
    entite         VARCHAR(50),
    entite_id      BIGINT,
    details        TEXT,
    cree_le        TIMESTAMP NOT NULL DEFAULT NOW()
);
