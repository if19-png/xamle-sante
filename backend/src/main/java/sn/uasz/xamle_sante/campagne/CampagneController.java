package sn.uasz.xamle_sante.campagne;

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
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle_sante.campagne.CampagneService.CampagneDto;
import sn.uasz.xamle_sante.common.Identite;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Agenda santé")
public class CampagneController {

    private final CampagneService service;

    @Operation(summary = "Campagnes de vaccination, dépistage et sensibilisation à venir")
    @GetMapping("/public/campagnes")
    public List<CampagneDto> aVenir() {
        return service.aVenir();
    }

    @Operation(summary = "S'abonner à une campagne : rappel SMS 24 h avant")
    @PostMapping("/campagnes/{id}/abonnement")
    @SecurityRequirement(name = "bearerAuth")
    public void sAbonner(@PathVariable("id") long id, @AuthenticationPrincipal Jwt jwt) {
        service.sAbonner(id, Identite.uid(jwt));
    }
}
