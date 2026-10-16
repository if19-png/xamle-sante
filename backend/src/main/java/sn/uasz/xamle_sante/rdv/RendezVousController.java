package sn.uasz.xamle_sante.rdv;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle_sante.common.Identite;
import sn.uasz.xamle_sante.rdv.RendezVousService.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Rendez-vous et paiement")
@SecurityRequirement(name = "bearerAuth")
public class RendezVousController {

    private final RendezVousService service;

    @Operation(summary = "Patient : demander un rendez-vous (vidéo = frais de dossier de 1 000 FCFA)")
    @PostMapping("/patient/rendez-vous")
    @ResponseStatus(HttpStatus.CREATED)
    public RdvDto creer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RdvRequest r) {
        return service.creer(Identite.uid(jwt), r);
    }

    @Operation(summary = "Patient : mes rendez-vous")
    @GetMapping("/patient/rendez-vous")
    public List<RdvDto> mesRdv(@AuthenticationPrincipal Jwt jwt) {
        return service.duPatient(Identite.uid(jwt));
    }

    @Operation(summary = "Patient : payer les frais de dossier (Wave sandbox simulé)")
    @PostMapping("/patient/paiements/{id}/payer")
    public RdvDto payer(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id) {
        return service.payer(Identite.uid(jwt), id);
    }

    @Operation(summary = "Professionnel : mes rendez-vous")
    @GetMapping("/pro/rendez-vous")
    public List<RdvDto> rdvPro(@AuthenticationPrincipal Jwt jwt) {
        return service.duProfessionnel(Identite.uid(jwt));
    }

    @Operation(summary = "Professionnel : confirmer un rendez-vous en présentiel")
    @PostMapping("/pro/rendez-vous/{id}/confirmer")
    public void confirmer(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id) {
        service.confirmerParPro(Identite.uid(jwt), id);
    }
}
