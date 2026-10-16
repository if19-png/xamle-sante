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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle_sante.annuaire.AnnuaireDtos.*;
import sn.uasz.xamle_sante.common.Identite;

import java.util.List;

@RestController
@RequestMapping("/api/admin/professionnels")
@RequiredArgsConstructor
@Tag(name = "Admin - validation des profils")
@SecurityRequirement(name = "bearerAuth")
public class AdminValidationController {

    private final AnnuaireService service;

    @Operation(summary = "Profils en attente de validation")
    @GetMapping("/en-attente")
    public List<ProfessionnelDto> enAttente() {
        return service.enAttente();
    }

    @Operation(summary = "Valider un profil (il devient public)")
    @PostMapping("/{id}/valider")
    public void valider(@PathVariable("id") long id, @AuthenticationPrincipal Jwt jwt) {
        service.valider(id, Identite.uid(jwt));
    }

    @Operation(summary = "Rejeter un profil avec un motif")
    @PostMapping("/{id}/rejeter")
    public void rejeter(@PathVariable("id") long id, @AuthenticationPrincipal Jwt jwt,
                        @Valid @RequestBody RejetRequest r) {
        service.rejeter(id, Identite.uid(jwt), r.motif());
    }
}
