-- Données de départ. ATTENTION : coordonnées et établissements "(démo)" = données de démonstration,
-- à remplacer par des données réelles (OpenStreetMap / Google Maps) avant une vraie mise en service.

INSERT INTO specialite (nom) VALUES
('Médecine générale'),('Pédiatrie'),('Gynécologie-obstétrique'),('Cardiologie'),('Dentisterie'),
('Ophtalmologie'),('Dermatologie'),('Chirurgie générale'),('ORL'),('Pharmacie'),
('Kinésithérapie'),('Psychiatrie');

INSERT INTO langue (nom) VALUES ('Français'),('Wolof'),('Diola'),('Mandingue'),('Pulaar'),('Anglais');

INSERT INTO quartier (nom, zone)
SELECT t.nom, ST_Multi(ST_Buffer(ST_SetSRID(ST_MakePoint(t.lon, t.lat), 4326)::geography, 450)::geometry)::geography
FROM (VALUES
 ('Escale',     -16.2748::float8, 12.5712::float8),
 ('Boucotte',   -16.2610, 12.5795),
 ('Lyndiane',   -16.2820, 12.5590),
 ('Kandé',      -16.2640, 12.5660),
 ('Santhiaba',  -16.2560, 12.5690),
 ('Néma',       -16.2700, 12.5560),
 ('Kenya',      -16.2690, 12.5740),
 ('Belfort',    -16.2590, 12.5630),
 ('Colobane',   -16.2780, 12.5780),
 ('Tilène',     -16.2760, 12.5600),
 ('Peyrissac',  -16.2700, 12.5700),
 ('Goumel',     -16.2500, 12.5900)
) AS t(nom, lon, lat);

INSERT INTO etablissement (nom, type, adresse, services, ouvert_24h, position) VALUES
('Hôpital Régional de Ziguinchor (Hôpital de la Paix)','HOPITAL','Ziguinchor','Urgences, maternité, pédiatrie, chirurgie, laboratoire',TRUE, ST_SetSRID(ST_MakePoint(-16.2689,12.5637),4326)::geography),
('Clinique Casamance Santé (démo)','CLINIQUE','Centre-ville','Urgences, consultations, imagerie',TRUE, ST_SetSRID(ST_MakePoint(-16.2741,12.5702),4326)::geography),
('Clinique du Fleuve (démo)','CLINIQUE','Kenya','Consultations spécialisées',FALSE, ST_SetSRID(ST_MakePoint(-16.2689,12.5748),4326)::geography),
('Centre de santé de Boucotte (démo)','DISPENSAIRE','Boucotte','Soins de base, vaccination, maternité',TRUE, ST_SetSRID(ST_MakePoint(-16.2602,12.5806),4326)::geography),
('Poste de santé de Lyndiane (démo)','DISPENSAIRE','Lyndiane','Soins de base, vaccination',FALSE, ST_SetSRID(ST_MakePoint(-16.2815,12.5589),4326)::geography),
('Centre de santé de Santhiaba (démo)','DISPENSAIRE','Santhiaba','Soins de base, dépistage',FALSE, ST_SetSRID(ST_MakePoint(-16.2560,12.5690),4326)::geography),
('Pharmacie Escale (démo)','PHARMACIE','Escale','Médicaments, pharmacie de garde',TRUE, ST_SetSRID(ST_MakePoint(-16.2750,12.5712),4326)::geography),
('Pharmacie Kandé (démo)','PHARMACIE','Kandé','Médicaments',FALSE, ST_SetSRID(ST_MakePoint(-16.2638,12.5655),4326)::geography);

INSERT INTO service_urgence (etablissement_id, type, disponible_24h)
SELECT id, 'URGENCES_GENERALES', TRUE FROM etablissement
WHERE nom IN ('Hôpital Régional de Ziguinchor (Hôpital de la Paix)','Clinique Casamance Santé (démo)','Centre de santé de Boucotte (démo)');
INSERT INTO service_urgence (etablissement_id, type, disponible_24h)
SELECT id, 'MATERNITE', TRUE FROM etablissement WHERE nom = 'Hôpital Régional de Ziguinchor (Hôpital de la Paix)';
INSERT INTO service_urgence (etablissement_id, type, disponible_24h)
SELECT id, 'PEDIATRIE', TRUE FROM etablissement WHERE nom = 'Hôpital Régional de Ziguinchor (Hôpital de la Paix)';
