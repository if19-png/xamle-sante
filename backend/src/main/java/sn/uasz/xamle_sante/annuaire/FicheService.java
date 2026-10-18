package sn.uasz.xamle_sante.annuaire;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Horaires et avis des spécialistes (compléments de la fiche publique). */
@Service
@RequiredArgsConstructor
public class FicheService {

    private final JdbcClient jdbc;

    public record HoraireDto(Integer jour, String debut, String fin) {}

    public record HoraireRequest(@NotNull Integer jour, @NotBlank String debut, @NotBlank String fin) {}

    public record AvisDto(Integer note, String commentaire, String auteur, String dateAvis) {}

    public record AvisReponse(Double moyenne, long total, List<AvisDto> avis) {}

    public record AvisRequest(@NotNull @Min(1) @Max(5) Integer note, @Size(max = 500) String commentaire) {}

    public record Complement(Double moyenne, long total, List<HoraireDto> horaires) {}

    // ---------- Lecture publique ----------

    public AvisReponse avis(long proId) {
        Double moyenne = jdbc.sql("""
                SELECT COALESCE(ROUND(AVG(note)::numeric, 1), 0)::float8
                FROM avis WHERE professionnel_id = :id
                """).param("id", proId).query(Double.class).single();
        Long total = jdbc.sql("SELECT COUNT(*) FROM avis WHERE professionnel_id = :id")
                .param("id", proId).query(Long.class).single();
        List<AvisDto> liste = jdbc.sql("""
                SELECT a.note, a.commentaire, u.prenom || ' ' || LEFT(u.nom, 1) || '.' AS auteur,
                       to_char(a.cree_le, 'DD/MM/YYYY') AS date_avis
                FROM avis a
                JOIN utilisateur u ON u.id = a.patient_id
                WHERE a.professionnel_id = :id
                ORDER BY a.cree_le DESC
                LIMIT 20
                """).param("id", proId).query(AvisDto.class).list();
        return new AvisReponse(moyenne, total, liste);
    }

    /** Note moyenne + horaires de plusieurs spécialistes en une seule requête (pour la liste de résultats). */
    public Map<Long, Complement> complements(List<Long> ids) {
        Map<Long, Complement> out = new LinkedHashMap<>();
        if (ids == null || ids.isEmpty()) return out;
        Map<Long, List<HoraireDto>> horaires = new LinkedHashMap<>();
        jdbc.sql("""
                SELECT professionnel_id, jour_semaine,
                       to_char(heure_debut, 'HH24:MI') AS debut, to_char(heure_fin, 'HH24:MI') AS fin
                FROM horaire
                WHERE professionnel_id IN (:ids)
                ORDER BY professionnel_id, jour_semaine, heure_debut
                """).param("ids", ids).query((rs, n) -> {
            horaires.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>())
                    .add(new HoraireDto(rs.getInt(2), rs.getString(3), rs.getString(4)));
            return 0;
        }).list();
        Map<Long, double[]> notes = new LinkedHashMap<>();
        jdbc.sql("""
                SELECT professionnel_id, AVG(note)::float8, COUNT(*)
                FROM avis
                WHERE professionnel_id IN (:ids)
                GROUP BY professionnel_id
                """).param("ids", ids).query((rs, n) -> {
            notes.put(rs.getLong(1), new double[]{rs.getDouble(2), rs.getLong(3)});
            return 0;
        }).list();
        for (Long id : ids) {
            double[] nt = notes.get(id);
            Double moyenne = nt == null ? 0.0 : Math.round(nt[0] * 10) / 10.0;
            long total = nt == null ? 0 : (long) nt[1];
            out.put(id, new Complement(moyenne, total, horaires.getOrDefault(id, List.of())));
        }
        return out;
    }

    // ---------- Patient : donner un avis ----------

    public void donnerAvis(long patientId, long proId, AvisRequest r) {
        Long ok = jdbc.sql("SELECT COUNT(*) FROM professionnel WHERE id = :id AND statut_validation = 'VALIDE'")
                .param("id", proId).query(Long.class).single();
        if (ok == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialiste introuvable");
        String commentaire = r.commentaire() == null || r.commentaire().isBlank() ? null : r.commentaire().trim();
        jdbc.sql("""
                INSERT INTO avis (professionnel_id, patient_id, note, commentaire)
                VALUES (:p, :u, :n, :c)
                ON CONFLICT (professionnel_id, patient_id)
                DO UPDATE SET note = EXCLUDED.note, commentaire = EXCLUDED.commentaire, cree_le = NOW()
                """).param("p", proId).param("u", patientId).param("n", r.note()).param("c", commentaire).update();
    }

    // ---------- Professionnel : ses horaires ----------

    private long monId(long utilisateurId) {
        return jdbc.sql("SELECT id FROM professionnel WHERE utilisateur_id = :u")
                .param("u", utilisateurId).query(Long.class).optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Créez d'abord votre profil professionnel."));
    }

    public List<HoraireDto> mesHoraires(long utilisateurId) {
        return jdbc.sql("""
                SELECT jour_semaine AS jour, to_char(heure_debut, 'HH24:MI') AS debut,
                       to_char(heure_fin, 'HH24:MI') AS fin
                FROM horaire
                WHERE professionnel_id = :p
                ORDER BY jour_semaine, heure_debut
                """).param("p", monId(utilisateurId)).query(HoraireDto.class).list();
    }

    @Transactional
    public List<HoraireDto> remplacerHoraires(long utilisateurId, List<HoraireRequest> liste) {
        long pro = monId(utilisateurId);
        for (HoraireRequest h : liste) {
            if (h.jour() < 1 || h.jour() > 7)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jour invalide (1 = lundi, 7 = dimanche).");
            if (!h.debut().matches("^([01]\\d|2[0-3]):[0-5]\\d$") || !h.fin().matches("^([01]\\d|2[0-3]):[0-5]\\d$"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Heure invalide (format HH:MM).");
            if (h.fin().compareTo(h.debut()) <= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure de fin doit suivre l'heure de début.");
        }
        jdbc.sql("DELETE FROM horaire WHERE professionnel_id = :p").param("p", pro).update();
        for (HoraireRequest h : liste) {
            jdbc.sql("""
                    INSERT INTO horaire (professionnel_id, jour_semaine, heure_debut, heure_fin)
                    VALUES (:p, :j, CAST(:d AS time), CAST(:f AS time))
                    """).param("p", pro).param("j", h.jour()).param("d", h.debut()).param("f", h.fin()).update();
        }
        return mesHoraires(utilisateurId);
    }
}
