package sn.uasz.xamle_sante.annuaire;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class AnnuaireDtos {
    private AnnuaireDtos() {}

    public record Specialite(Integer id, String nom) {}

    public record Langue(Integer id, String nom) {}

    public record Quartier(Integer id, String nom, Double latitude, Double longitude) {}

    public record EtablissementDto(Long id, String nom, String type, String adresse, String telephone,
                                   String services, Boolean ouvert24h, Double latitude, Double longitude,
                                   Double distanceM) {}

    public record ProfessionnelDto(Long id, String nom, String prenom, String specialite, Integer specialiteId,
                                   Long etablissementId, String etablissement, String telephone, String bio,
                                   String photoUrl, Boolean consultationVideo, String statutValidation,
                                   String motifRejet, Double latitude, Double longitude, Double distanceM,
                                   String langues) {}

    public record ProfilRequest(@NotNull Integer specialiteId, Long etablissementId, String bio,
                                String telephonePro, String photoUrl, Double latitude, Double longitude,
                                List<Integer> langueIds, Boolean consultationVideo) {}

    public record RejetRequest(@NotBlank String motif) {}
}
