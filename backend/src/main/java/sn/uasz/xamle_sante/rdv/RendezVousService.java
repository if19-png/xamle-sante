package sn.uasz.xamle_sante.rdv;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RendezVousService {

    private final JdbcClient jdbc;

    private static final int FRAIS_DOSSIER = 1000; // FCFA, consultation vidéo

    private static final String SELECT = """
            SELECT r.id, r.professionnel_id, pu.prenom || ' ' || pu.nom AS professionnel,
                   pa.prenom || ' ' || pa.nom AS patient, r.date_heure, r.type, r.statut,
                   r.lien_video, r.motif, pm.id AS paiement_id, pm.statut AS paiement_statut, pm.montant
            FROM rendez_vous r
            JOIN professionnel p ON p.id = r.professionnel_id
            JOIN utilisateur pu ON pu.id = p.utilisateur_id
            JOIN utilisateur pa ON pa.id = r.patient_id
            LEFT JOIN paiement pm ON pm.rendez_vous_id = r.id
            """;

    public record RdvRequest(@NotNull Long professionnelId, @NotNull LocalDateTime dateHeure,
                             @NotBlank @Pattern(regexp = "PRESENTIEL|VIDEO") String type, String motif) {}

    public record RdvDto(Long id, Long professionnelId, String professionnel, String patient,
                         LocalDateTime dateHeure, String type, String statut, String lienVideo, String motif,
                         Long paiementId, String paiementStatut, Integer montant) {}

    @Transactional
    public RdvDto creer(long patientUid, RdvRequest r) {
        Optional<Boolean> video = jdbc.sql("SELECT consultation_video FROM professionnel "
                        + "WHERE id = :id AND statut_validation = 'VALIDE'")
                .param("id", r.professionnelId()).query(Boolean.class).optional();
        if (video.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialiste introuvable ou non validé");
        }
        if ("VIDEO".equals(r.type()) && !video.get()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce spécialiste ne propose pas de consultation vidéo");
        }
        if (r.dateHeure().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date doit être dans le futur");
        }
        long id = jdbc.sql("INSERT INTO rendez_vous (patient_id, professionnel_id, date_heure, type, motif) "
                        + "VALUES (:p, :pro, :d, :t, :m) RETURNING id")
                .param("p", patientUid).param("pro", r.professionnelId()).param("d", r.dateHeure())
                .param("t", r.type()).param("m", r.motif()).query(Long.class).single();
        if ("VIDEO".equals(r.type())) {
            jdbc.sql("INSERT INTO paiement (rendez_vous_id, montant) VALUES (:id, :m)")
                    .param("id", id).param("m", FRAIS_DOSSIER).update();
        }
        return trouver(id);
    }

    /**
     * Paiement Wave SIMULÉ (sandbox interne) : la référence est générée localement.
     * Un paiement réussi confirme le rendez-vous et crée le lien de visioconférence.
     */
    @Transactional
    public RdvDto payer(long patientUid, long paiementId) {
        String reference = "WAVE-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int n = jdbc.sql("UPDATE paiement SET statut = 'PAYE', reference_wave = :ref, maj_le = LOCALTIMESTAMP "
                        + "WHERE id = :id AND statut = 'EN_ATTENTE' "
                        + "AND rendez_vous_id IN (SELECT id FROM rendez_vous WHERE patient_id = :uid)")
                .param("ref", reference).param("id", paiementId).param("uid", patientUid).update();
        if (n == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable ou déjà réglé");
        }
        long rdvId = jdbc.sql("SELECT rendez_vous_id FROM paiement WHERE id = :id")
                .param("id", paiementId).query(Long.class).single();
        String lien = "https://meet.jit.si/XamleSante-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.sql("UPDATE rendez_vous SET statut = 'CONFIRME', lien_video = :lien WHERE id = :id")
                .param("lien", lien).param("id", rdvId).update();
        return trouver(rdvId);
    }

    public void confirmerParPro(long proUid, long rdvId) {
        int n = jdbc.sql("UPDATE rendez_vous SET statut = 'CONFIRME' WHERE id = :id AND statut = 'DEMANDE' "
                        + "AND type = 'PRESENTIEL' AND professionnel_id IN "
                        + "(SELECT id FROM professionnel WHERE utilisateur_id = :uid)")
                .param("id", rdvId).param("uid", proUid).update();
        if (n == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rendez-vous introuvable ou non confirmable");
        }
    }

    public List<RdvDto> duPatient(long uid) {
        return jdbc.sql(SELECT + "WHERE r.patient_id = :uid ORDER BY r.date_heure DESC")
                .param("uid", uid).query(RdvDto.class).list();
    }

    public List<RdvDto> duProfessionnel(long uid) {
        return jdbc.sql(SELECT + "WHERE p.utilisateur_id = :uid ORDER BY r.date_heure DESC")
                .param("uid", uid).query(RdvDto.class).list();
    }

    private RdvDto trouver(long id) {
        return jdbc.sql(SELECT + "WHERE r.id = :id").param("id", id).query(RdvDto.class).single();
    }
}
