package sn.uasz.xamle_sante.annuaire;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle_sante.annuaire.AnnuaireDtos.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Annuaire (public)")
public class PublicAnnuaireController {

    private final AnnuaireService service;

    @Operation(summary = "Liste des spécialités")
    @GetMapping("/specialites")
    public List<Specialite> specialites() {
        return service.specialites();
    }

    @Operation(summary = "Liste des langues parlées")
    @GetMapping("/langues")
    public List<Langue> langues() {
        return service.langues();
    }

    @Operation(summary = "Liste des quartiers")
    @GetMapping("/quartiers")
    public List<Quartier> quartiers() {
        return service.quartiers();
    }

    @Operation(summary = "Établissements (hôpitaux, cliniques, pharmacies...), triés par distance si lat/lon fournis")
    @GetMapping("/etablissements")
    public List<EtablissementDto> etablissements(
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lon", required = false) Double lon,
            @RequestParam(name = "type", required = false) String type) {
        return service.etablissements(lat, lon, type);
    }

    @Operation(summary = "Recherche de spécialistes validés (spécialité, langue, texte, rayon, disponibilité), triés par distance")
    @GetMapping("/professionnels")
    public List<ProfessionnelDto> rechercher(
            @RequestParam(name = "specialiteId", required = false) Integer specialiteId,
            @RequestParam(name = "langueId", required = false) Integer langueId,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lon", required = false) Double lon,
            @RequestParam(name = "rayonM", required = false) Double rayonM,
            @RequestParam(name = "disponible", defaultValue = "false") boolean disponible) {
        return service.rechercher(specialiteId, langueId, q, lat, lon, rayonM, disponible);
    }

    @Operation(summary = "Fiche d'un spécialiste validé")
    @GetMapping("/professionnels/{id}")
    public ProfessionnelDto fiche(@PathVariable("id") long id) {
        return service.professionnelPublic(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialiste introuvable"));
    }
}
