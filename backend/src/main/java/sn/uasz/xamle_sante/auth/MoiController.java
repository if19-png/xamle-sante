package sn.uasz.xamle_sante.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Profil")
@SecurityRequirement(name = "bearerAuth")
public class MoiController {

    @Operation(summary = "Utilisateur connecté (d'après le token)")
    @GetMapping
    public Map<String, Object> moi(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "email", jwt.getSubject(),
                "role", jwt.getClaimAsString("role"),
                "id", jwt.getClaim("uid")
        );
    }
}