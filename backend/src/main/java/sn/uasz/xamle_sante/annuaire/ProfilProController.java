package sn.uasz.xamle_sante.annuaire;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle_sante.annuaire.AnnuaireDtos.*;
import sn.uasz.xamle_sante.common.Identite;

@RestController
@RequestMapping("/api/pro/mon-profil")
@RequiredArgsConstructor
@Tag(name = "Espace professionnel")
@SecurityRequirement(name = "bearerAuth")
public class ProfilProController {

    private final AnnuaireService service;

    @Operation(summary = "Mon profil professionnel (avec son statut de validation)")
    @GetMapping
    public ProfessionnelDto monProfil(@AuthenticationPrincipal Jwt jwt) {
        return service.monProfil(Identite.uid(jwt))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun profil créé"));
    }

    @Operation(summary = "Créer ou modifier mon profil (repasse en BROUILLON)")
    @PutMapping
    public ProfessionnelDto sauvegarder(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfilRequest r) {
        return service.sauvegarderProfil(Identite.uid(jwt), r);
    }

    @Operation(summary = "Soumettre mon profil à la validation de l'administration")
    @PostMapping("/soumettre")
    public void soumettre(@AuthenticationPrincipal Jwt jwt) {
        service.soumettre(Identite.uid(jwt));
    }
}
