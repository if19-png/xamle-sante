package sn.uasz.xamle_sante.epidemio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Surveillance épidémiologique.
 * ANONYMAT : la table signalement n'a AUCUNE colonne vers l'utilisateur, et les cartes/exports
 * n'affichent un quartier que s'il compte au moins "seuil-anonymat" signalements validés.
 */
@Service
@RequiredArgsConstructor
public class EpidemioService {

    private final JdbcClient jdbc;

    @Value("${app.epidemio.seuil-anonymat:3}")
    private int seuilAnonymat;
    @Value("${app.epidemio.seuil-risque:5}")
    private int seuilRisque;

    public record SignalementRequest(
            @NotNull Integer quartierId,
            @NotBlank @Pattern(regexp = "FIEVRE|DIARRHEE|TOUX|VOMISSEMENTS|ERUPTION|AUTRE") String symptome,
            @Size(max = 500) String description) {}

    public record SignalementModeration(Long id, String quartier, String symptome, String description,
                                        String statut, LocalDateTime creeLe) {}

    public record ZoneChaleur(Integer quartierId, String quartier, Double latitude, Double longitude, Long nb) {}

    public record AlerteRequest(Integer quartierId,
                                @NotBlank @Pattern(regexp = "INFO|VIGILANCE|ALERTE") String niveau,
                                @NotBlank String titre, @NotBlank String message) {}

    public record AlerteDto(Long id, String quartier, String niveau, String titre, String message,
                            LocalDateTime creeLe) {}

    public void signaler(SignalementRequest r) {
        jdbc.sql("INSERT INTO signalement (quartier_id, symptome, description) VALUES (:q, :s, :d)")
                .param("q", r.quartierId()).param("s", r.symptome()).param("d", r.description()).update();
    }

    public List<SignalementModeration> aModerer(String statut) {
        return jdbc.sql("""
                SELECT s.id, q.nom AS quartier, s.symptome, s.description, s.statut, s.cree_le
                FROM signalement s JOIN quartier q ON q.id = s.quartier_id
                WHERE s.statut = :statut ORDER BY s.cree_le DESC LIMIT 200""")
                .param("statut", statut).query(SignalementModeration.class).list();
    }

    public void moderer(long id, long uid, boolean valide) {
        int n = jdbc.sql("UPDATE signalement SET statut = :statut, modere_par = :uid, modere_le = LOCALTIMESTAMP "
                        + "WHERE id = :id AND statut = 'EN_ATTENTE'")
                .param("statut", valide ? "VALIDE" : "REJETE").param("uid", uid).param("id", id).update();
        if (n == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Signalement introuvable ou déjà modéré");
        }
    }

    /** Quartiers dont le nombre de signalements validés atteint le seuil donné. */
    public List<ZoneChaleur> zones(int jours, String symptome, int seuil) {
        Map<String, Object> p = new HashMap<>();
        p.put("jours", jours);
        p.put("seuil", seuil);
        StringBuilder sql = new StringBuilder("""
                SELECT q.id AS quartier_id, q.nom AS quartier,
                       ST_Y(ST_Centroid(q.zone::geometry)) AS latitude,
                       ST_X(ST_Centroid(q.zone::geometry)) AS longitude,
                       COUNT(*) AS nb
                FROM signalement s JOIN quartier q ON q.id = s.quartier_id
                WHERE s.statut = 'VALIDE' AND s.cree_le > LOCALTIMESTAMP - (:jours * INTERVAL '1 day') """);
        if (symptome != null && !symptome.isBlank()) {
            sql.append("AND s.symptome = :symptome ");
            p.put("symptome", symptome.trim().toUpperCase());
        }
        sql.append("GROUP BY q.id HAVING COUNT(*) >= :seuil ORDER BY nb DESC");
        return jdbc.sql(sql.toString()).params(p).query(ZoneChaleur.class).list();
    }

    public List<ZoneChaleur> carteChaleur(int jours, String symptome) {
        return zones(jours, symptome, seuilAnonymat);
    }

    public List<ZoneChaleur> zonesARisque(int jours) {
        return zones(jours, null, seuilRisque);
    }

    public void creerAlerte(AlerteRequest r, long uid) {
        jdbc.sql("INSERT INTO alerte (quartier_id, niveau, titre, message, cree_par) VALUES (:q, :n, :t, :m, :u)")
                .param("q", r.quartierId()).param("n", r.niveau()).param("t", r.titre())
                .param("m", r.message()).param("u", uid).update();
    }

    public List<AlerteDto> alertesActives() {
        return jdbc.sql("""
                SELECT a.id, q.nom AS quartier, a.niveau, a.titre, a.message, a.cree_le
                FROM alerte a LEFT JOIN quartier q ON q.id = a.quartier_id
                WHERE a.expire_le IS NULL OR a.expire_le > LOCALTIMESTAMP
                ORDER BY a.cree_le DESC LIMIT 50""").query(AlerteDto.class).list();
    }

    /** Export CSV agrégé et anonymisé (seuil appliqué). */
    public String exportCsv(int jours) {
        List<Map<String, Object>> rows = jdbc.sql("""
                SELECT q.nom AS quartier, s.symptome, COUNT(*) AS nb
                FROM signalement s JOIN quartier q ON q.id = s.quartier_id
                WHERE s.statut = 'VALIDE' AND s.cree_le > LOCALTIMESTAMP - (:jours * INTERVAL '1 day')
                GROUP BY q.nom, s.symptome HAVING COUNT(*) >= :seuil
                ORDER BY q.nom, s.symptome""")
                .param("jours", jours).param("seuil", seuilAnonymat).query().listOfRows();
        StringBuilder sb = new StringBuilder("\uFEFFquartier;symptome;nombre_signalements;periode_jours\n");
        for (Map<String, Object> r : rows) {
            sb.append(r.get("quartier")).append(';').append(r.get("symptome")).append(';')
                    .append(r.get("nb")).append(';').append(jours).append('\n');
        }
        return sb.toString();
    }
}
