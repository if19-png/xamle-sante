-- Horaires affichés sur la page des établissements + numéros de démonstration.
-- Les numéros "33 000 00 0X" sont FICTIFS (plage non attribuée) : à remplacer par les vrais numéros.
ALTER TABLE etablissement ADD COLUMN IF NOT EXISTS horaires VARCHAR(200);

UPDATE etablissement SET
    horaires = CASE
        WHEN ouvert_24h THEN 'Ouvert 24h/24, 7j/7'
        WHEN type = 'PHARMACIE' THEN 'Lun-Sam : 08h30 - 20h00'
        WHEN type = 'CLINIQUE' THEN 'Lun-Sam : 08h00 - 18h00'
        ELSE 'Lun-Ven : 08h00 - 17h00'
    END,
    telephone = COALESCE(telephone, CASE nom
        WHEN 'Hôpital Régional de Ziguinchor (Hôpital de la Paix)' THEN '33 000 00 01'
        WHEN 'Clinique Casamance Santé (démo)' THEN '33 000 00 02'
        WHEN 'Clinique du Fleuve (démo)' THEN '33 000 00 03'
        WHEN 'Centre de santé de Boucotte (démo)' THEN '33 000 00 04'
        WHEN 'Poste de santé de Lyndiane (démo)' THEN '33 000 00 05'
        WHEN 'Centre de santé de Santhiaba (démo)' THEN '33 000 00 06'
        WHEN 'Pharmacie Escale (démo)' THEN '33 000 00 07'
        WHEN 'Pharmacie Kandé (démo)' THEN '33 000 00 08'
    END);
