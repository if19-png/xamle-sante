package sn.uasz.xamle_sante.annuaire;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle_sante.annuaire.AnnuaireDtos.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Annuaire géolocalisé. Les requêtes spatiales passent par du SQL natif (PostGIS) :
 * pas besoin de Hibernate Spatial.
 */
@Service
@RequiredArgsConstructor
public class AnnuaireService {

    private final JdbcClient jdbc;

    private static final String POINT = "ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography";
    private static final String SANS_DISTANCE = "CAST(NULL AS double precision)";

    private static final String PRO_SELECT = """
            SELECT p.id, u.nom, u.prenom, s.nom AS specialite, p.specialite_id, p.etablissement_id,
                   e.nom AS etablissement, COALESCE(p.telephone_pro, u.telephone) AS telephone,
                   p.bio, p.photo_url, p.consultation_video, p.statut_validation, p.motif_rejet,
                   ST_Y(COALESCE(p.position, e.position)::geometry) AS latitude,
                   ST_X(COALESCE(p.position, e.position)::geometry) AS longitude,
                   %s AS distance_m,
                   (SELECT string_agg(l.nom, ', ' ORDER BY l.nom) FROM professionnel_langue pl
                      JOIN langue l ON l.id = pl.langue_id WHERE pl.professionnel_id = p.id) AS langues
            FROM professionnel p
            JOIN utilisateur u ON u.id = p.utilisateur_id
            JOIN specialite s ON s.id = p.specialite_id
            LEFT JOIN etablissement e ON e.id = p.etablissement_id
            """;

    // ---------- Référentiels ----------

    public List<Specialite> specialites() {
        return jdbc.sql("SELECT id, nom FROM specialite ORDER BY nom").query(Specialite.class).list();
    }

    public List<Langue> langues() {
        return jdbc.sql("SELECT id, nom FROM langue ORDER BY nom").query(Langue.class).list();
    }

    public List<Quartier> quartiers() {
        return jdbc.sql("""
                SELECT q.id, q.nom,
                       ST_Y(ST_Centroid(q.zone::geometry)) AS latitude,
                       ST_X(ST_Centroid(q.zone::geometry)) AS longitude
                FROM quartier q ORDER BY q.nom""").query(Quartier.class).list();
    }

    // ---------- Établissements ----------

    public List<EtablissementDto> etablissements(Double lat, Double lon, String type) {
        boolean geo = lat != null && lon != null;
        Map<String, Object> p = new HashMap<>();
        StringBuilder sql = new StringBuilder("""
                SELECT e.id, e.nom, e.type, e.adresse, e.telephone, e.services, e.ouvert_24h AS ouvert24h, e.horaires,
                       ST_Y(e.position::geometry) AS latitude, ST_X(e.position::geometry) AS longitude,
                """);
        if (geo) {
            sql.append("ST_Distance(e.position, ").append(POINT).append(") AS distance_m ");
            p.put("lat", lat);
            p.put("lon", lon);
        } else {
            sql.append(SANS_DISTANCE).append(" AS distance_m ");
        }
        sql.append("FROM etablissement e WHERE 1 = 1 ");
        if (type != null && !type.isBlank()) {
            sql.append("AND e.type = :type ");
            p.put("type", type.trim().toUpperCase());
        }
        sql.append(geo ? "ORDER BY distance_m " : "ORDER BY e.nom ").append("LIMIT 100");
        return jdbc.sql(sql.toString()).params(p).query(EtablissementDto.class).list();
    }

    // ---------- Recherche de professionnels (publics : profils VALIDES uniquement) ----------

    public List<ProfessionnelDto> rechercher(Integer specialiteId, Integer langueId, String q,
                                             Double lat, Double lon, Double rayonM, boolean disponible) {
        boolean geo = lat != null && lon != null;
        Map<String, Object> p = new HashMap<>();
        String distance = geo ? "ST_Distance(COALESCE(p.position, e.position), " + POINT + ")" : SANS_DISTANCE;
        StringBuilder sql = new StringBuilder(PRO_SELECT.formatted(distance));
        sql.append("WHERE p.statut_validation = 'VALIDE' ");
        if (geo) {
            p.put("lat", lat);
            p.put("lon", lon);
        }
        if (specialiteId != null) {
            sql.append("AND p.specialite_id = :specialiteId ");
            p.put("specialiteId", specialiteId);
        }
        if (langueId != null) {
            sql.append("AND EXISTS (SELECT 1 FROM professionnel_langue x WHERE x.professionnel_id = p.id "
                    + "AND x.langue_id = :langueId) ");
            p.put("langueId", langueId);
        }
        if (q != null && !q.isBlank()) {
            sql.append("AND (u.nom ILIKE :q OR u.prenom ILIKE :q OR s.nom ILIKE :q OR e.nom ILIKE :q) ");
            p.put("q", "%" + q.trim() + "%");
        }
        if (geo && rayonM != null) {
            sql.append("AND ST_DWithin(COALESCE(p.position, e.position), ").append(POINT).append(", :rayon) ");
            p.put("rayon", rayonM);
        }
        if (disponible) {
            sql.append("AND EXISTS (SELECT 1 FROM horaire h WHERE h.professionnel_id = p.id "
                    + "AND h.jour_semaine = EXTRACT(ISODOW FROM (NOW() AT TIME ZONE 'Africa/Dakar'))::int "
                    + "AND (NOW() AT TIME ZONE 'Africa/Dakar')::time BETWEEN h.heure_debut AND h.heure_fin) ");
        }
        sql.append(geo ? "ORDER BY distance_m NULLS LAST, u.nom " : "ORDER BY u.nom ").append("LIMIT 100");
        return jdbc.sql(sql.toString()).params(p).query(ProfessionnelDto.class).list();
    }

    public Optional<ProfessionnelDto> professionnelPublic(long id) {
        return jdbc.sql(PRO_SELECT.formatted(SANS_DISTANCE) + "WHERE p.id = :id AND p.statut_validation = 'VALIDE'")
                .param("id", id).query(ProfessionnelDto.class).optional();
    }

    // ---------- Espace professionnel ----------

    public Optional<ProfessionnelDto> monProfil(long uid) {
        return jdbc.sql(PRO_SELECT.formatted(SANS_DISTANCE) + "WHERE p.utilisateur_id = :uid")
                .param("uid", uid).query(ProfessionnelDto.class).optional();
    }

    /** Toute modification remet le profil en BROUILLON : il devra être soumis puis revalidé. */
    @Transactional
    public ProfessionnelDto sauvegarderProfil(long uid, ProfilRequest r) {
        boolean point = r.latitude() != null && r.longitude() != null;
        String pointSql = point ? POINT : "NULL";
        Map<String, Object> p = new HashMap<>();
        p.put("uid", uid);
        p.put("sp", r.specialiteId());
        p.put("et", r.etablissementId());
        p.put("bio", r.bio());
        p.put("tel", r.telephonePro());
        p.put("photo", r.photoUrl());
        p.put("video", Boolean.TRUE.equals(r.consultationVideo()));
        if (point) {
            p.put("lat", r.latitude());
            p.put("lon", r.longitude());
        }

        Optional<Long> existant = jdbc.sql("SELECT id FROM professionnel WHERE utilisateur_id = :uid")
                .param("uid", uid).query(Long.class).optional();
        long id;
        if (existant.isPresent()) {
            id = existant.get();
            p.put("id", id);
            jdbc.sql("UPDATE professionnel SET specialite_id = :sp, etablissement_id = :et, bio = :bio, "
                            + "telephone_pro = :tel, photo_url = :photo, consultation_video = :video, "
                            + "position = " + pointSql + ", statut_validation = 'BROUILLON', motif_rejet = NULL "
                            + "WHERE id = :id")
                    .params(p).update();
        } else {
            id = jdbc.sql("INSERT INTO professionnel (utilisateur_id, specialite_id, etablissement_id, bio, "
                            + "telephone_pro, photo_url, consultation_video, position) "
                            + "VALUES (:uid, :sp, :et, :bio, :tel, :photo, :video, " + pointSql + ") RETURNING id")
                    .params(p).query(Long.class).single();
        }

        jdbc.sql("DELETE FROM professionnel_langue WHERE professionnel_id = :id").param("id", id).update();
        if (r.langueIds() != null) {
            for (Integer langueId : r.langueIds()) {
                jdbc.sql("INSERT INTO professionnel_langue (professionnel_id, langue_id) VALUES (:id, :l) "
                                + "ON CONFLICT DO NOTHING")
                        .param("id", id).param("l", langueId).update();
            }
        }
        return monProfil(uid).orElseThrow();
    }

    public void soumettre(long uid) {
        int n = jdbc.sql("UPDATE professionnel SET statut_validation = 'EN_ATTENTE', motif_rejet = NULL "
                        + "WHERE utilisateur_id = :uid AND statut_validation IN ('BROUILLON', 'REJETE')")
                .param("uid", uid).update();
        if (n == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Profil inexistant, déjà soumis ou déjà validé");
        }
    }

    // ---------- Workflow de validation (admin) ----------

    public List<ProfessionnelDto> enAttente() {
        return jdbc.sql(PRO_SELECT.formatted(SANS_DISTANCE)
                + "WHERE p.statut_validation = 'EN_ATTENTE' ORDER BY p.cree_le")
                .query(ProfessionnelDto.class).list();
    }

    @Transactional
    public void valider(long id, long adminUid) {
        int n = jdbc.sql("UPDATE professionnel SET statut_validation = 'VALIDE', valide_par = :admin, "
                        + "valide_le = LOCALTIMESTAMP, motif_rejet = NULL "
                        + "WHERE id = :id AND statut_validation = 'EN_ATTENTE'")
                .param("admin", adminUid).param("id", id).update();
        if (n == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profil introuvable ou pas en attente");
        }
        notifier(id, "Profil validé", "Votre profil est maintenant publié dans l'annuaire.");
    }

    @Transactional
    public void rejeter(long id, long adminUid, String motif) {
        int n = jdbc.sql("UPDATE professionnel SET statut_validation = 'REJETE', valide_par = :admin, "
                        + "valide_le = LOCALTIMESTAMP, motif_rejet = :motif "
                        + "WHERE id = :id AND statut_validation = 'EN_ATTENTE'")
                .param("admin", adminUid).param("motif", motif).param("id", id).update();
        if (n == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profil introuvable ou pas en attente");
        }
        notifier(id, "Profil rejeté", "Motif : " + motif);
    }

    private void notifier(long professionnelId, String titre, String contenu) {
        jdbc.sql("INSERT INTO notification (utilisateur_id, titre, contenu) "
                        + "SELECT utilisateur_id, :titre, :contenu FROM professionnel WHERE id = :id")
                .param("titre", titre).param("contenu", contenu).param("id", professionnelId).update();
    }
}
