package sn.uasz.xamle_sante.annuaire;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle_sante.annuaire.FicheService.*;
import sn.uasz.xamle_sante.common.Identite;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Fiche spécialiste : horaires et avis")
public class FicheController {

    private final FicheService service;

    @Operation(summary = "Note moyenne et horaires de plusieurs spécialistes (ids séparés par des virgules)")
    @GetMapping("/public/professionnels/complements")
    public Map<Long, Complement> complements(@RequestParam(name = "ids") List<Long> ids) {
        return service.complements(ids);
    }

    @Operation(summary = "Avis patients d'un spécialiste (moyenne, total, derniers commentaires)")
    @GetMapping("/public/professionnels/{id}/avis")
    public AvisReponse avis(@PathVariable("id") long id) {
        return service.avis(id);
    }

    @Operation(summary = "Donner ou modifier mon avis (1 à 5) sur un spécialiste validé")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/patient/professionnels/{id}/avis")
    public void donnerAvis(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id,
                           @Valid @RequestBody AvisRequest r) {
        service.donnerAvis(Identite.uid(jwt), id, r);
    }

    @Operation(summary = "Mes horaires de consultation")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/pro/horaires")
    public List<HoraireDto> mesHoraires(@AuthenticationPrincipal Jwt jwt) {
        return service.mesHoraires(Identite.uid(jwt));
    }

    @Operation(summary = "Remplacer mes horaires (jour 1 = lundi ... 7 = dimanche, heures HH:MM)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/pro/horaires")
    public List<HoraireDto> remplacerHoraires(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody List<@Valid HoraireRequest> liste) {
        return service.remplacerHoraires(Identite.uid(jwt), liste);
    }
}
