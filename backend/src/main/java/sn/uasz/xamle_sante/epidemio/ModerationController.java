package sn.uasz.xamle_sante.epidemio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle_sante.common.Identite;
import sn.uasz.xamle_sante.epidemio.EpidemioService.SignalementModeration;

import java.util.List;

@RestController
@RequestMapping("/api/moderation/signalements")
@RequiredArgsConstructor
@Tag(name = "Modération médicale")
@SecurityRequirement(name = "bearerAuth")
public class ModerationController {

    private final EpidemioService service;

    @Operation(summary = "Signalements à modérer (statut EN_ATTENTE par défaut)")
    @GetMapping
    public List<SignalementModeration> liste(@RequestParam(name = "statut", defaultValue = "EN_ATTENTE") String statut) {
        return service.aModerer(statut);
    }

    @Operation(summary = "Valider un signalement (il compte dans la carte de chaleur)")
    @PostMapping("/{id}/valider")
    public void valider(@PathVariable("id") long id, @AuthenticationPrincipal Jwt jwt) {
        service.moderer(id, Identite.uid(jwt), true);
    }

    @Operation(summary = "Rejeter un signalement")
    @PostMapping("/{id}/rejeter")
    public void rejeter(@PathVariable("id") long id, @AuthenticationPrincipal Jwt jwt) {
        service.moderer(id, Identite.uid(jwt), false);
    }
}
