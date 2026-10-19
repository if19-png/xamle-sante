package sn.uasz.xamle_sante.campagne;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle_sante.sms.SmsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampagneService {

    private final JdbcClient jdbc;
    private final SmsService sms;

    private static final String POINT = "ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography";
    private static final String SELECT = """
            SELECT c.id, c.type, c.titre, c.description, c.lieu,
                   ST_Y(c.position::geometry) AS latitude, ST_X(c.position::geometry) AS longitude,
                   c.date_debut, c.date_fin
            FROM campagne c
            """;

    public record CampagneRequest(
            @NotBlank @Pattern(regexp = "VACCINATION|DEPISTAGE|SENSIBILISATION") String type,
            @NotBlank String titre, String description, String lieu,
            Double latitude, Double longitude,
            @NotNull LocalDateTime dateDebut, LocalDateTime dateFin) {}

    public record CampagneDto(Long id, String type, String titre, String description, String lieu,
                              Double latitude, Double longitude, LocalDateTime dateDebut, LocalDateTime dateFin) {}

    public record RappelDu(Long id, String telephone, String titre, LocalDateTime dateDebut, String lieu) {}

    public CampagneDto creer(CampagneRequest r, long uid) {
        boolean point = r.latitude() != null && r.longitude() != null;
        Map<String, Object> p = new HashMap<>();
        p.put("type", r.type());
        p.put("titre", r.titre());
        p.put("description", r.description());
        p.put("lieu", r.lieu());
        p.put("debut", r.dateDebut());
        p.put("fin", r.dateFin());
        p.put("uid", uid);
        if (point) {
            p.put("lat", r.latitude());
            p.put("lon", r.longitude());
        }
        long id = jdbc.sql("INSERT INTO campagne (type, titre, description, lieu, position, date_debut, date_fin, cree_par) "
                        + "VALUES (:type, :titre, :description, :lieu, " + (point ? POINT : "NULL")
                        + ", :debut, :fin, :uid) RETURNING id")
                .params(p).query(Long.class).single();
        return jdbc.sql(SELECT + "WHERE c.id = :id").param("id", id).query(CampagneDto.class).single();
    }

    public List<CampagneDto> aVenir() {
        return jdbc.sql(SELECT + "WHERE COALESCE(c.date_fin, c.date_debut) >= LOCALTIMESTAMP ORDER BY c.date_debut")
                .query(CampagneDto.class).list();
    }

    /** Abonnement : planifie un SMS de rappel 24 h avant le début (ou tout de suite si c'est déjà moins de 24 h). */
    @Transactional
    public void sAbonner(long campagneId, long uid) {
        Integer existe = jdbc.sql("SELECT COUNT(*) FROM campagne WHERE id = :id")
                .param("id", campagneId).query(Integer.class).single();
        if (existe == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campagne introuvable");
        }
        jdbc.sql("INSERT INTO abonnement_campagne (campagne_id, utilisateur_id) VALUES (:c, :u) ON CONFLICT DO NOTHING")
                .param("c", campagneId).param("u", uid).update();
        jdbc.sql("INSERT INTO rappel_sms (campagne_id, utilisateur_id, envoi_prevu_le) "
                        + "SELECT c.id, :u, GREATEST(c.date_debut - INTERVAL '24 hours', LOCALTIMESTAMP) "
                        + "FROM campagne c WHERE c.id = :c ON CONFLICT (campagne_id, utilisateur_id) DO NOTHING")
                .param("c", campagneId).param("u", uid).update();
    }

    // ---------- Rappels SMS planifiés ----------

    @Scheduled(fixedDelay = 60_000, initialDelay = 20_000)
    public void tacheRappels() {
        try {
            envoyerRappelsDus();
        } catch (Exception e) {
            log.warn("Échec de la tâche de rappels : {}", e.getMessage());
        }
    }

    /** Envoie tous les rappels dont l'heure est passée. Retourne le nombre traité. */
    public int envoyerRappelsDus() {
        List<RappelDu> dus = jdbc.sql("""
                SELECT r.id, u.telephone, c.titre, c.date_debut, c.lieu
                FROM rappel_sms r
                JOIN campagne c ON c.id = r.campagne_id
                JOIN utilisateur u ON u.id = r.utilisateur_id
                WHERE r.statut = 'PLANIFIE' AND r.envoi_prevu_le <= LOCALTIMESTAMP
                ORDER BY r.id LIMIT 100""").query(RappelDu.class).list();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
        for (RappelDu d : dus) {
            String message = "Rappel Xamlé Santé : « " + d.titre() + " » le " + d.dateDebut().format(fmt)
                    + (d.lieu() != null ? ", " + d.lieu() : "") + ".";
            SmsService.Resultat res = sms.envoyer(d.telephone(), message);
            jdbc.sql("UPDATE rappel_sms SET statut = :s, envoye_le = LOCALTIMESTAMP WHERE id = :id")
                    .param("s", res.envoye() ? "ENVOYE" : "ECHEC").param("id", d.id()).update();
        }
        return dus.size();
    }
}
