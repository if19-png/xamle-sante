package sn.uasz.xamle_sante.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle_sante.auth.RegisterRequest;
import sn.uasz.xamle_sante.campagne.CampagneService;
import sn.uasz.xamle_sante.campagne.CampagneService.CampagneDto;
import sn.uasz.xamle_sante.campagne.CampagneService.CampagneRequest;
import sn.uasz.xamle_sante.common.Identite;
import sn.uasz.xamle_sante.epidemio.EpidemioService;
import sn.uasz.xamle_sante.epidemio.EpidemioService.AlerteRequest;
import sn.uasz.xamle_sante.Utilisateur.Utilisateur;
import sn.uasz.xamle_sante.Utilisateur.UtilisateurRepository;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration santé publique")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final DashboardService dashboard;
    private final EpidemioService epidemio;
    private final CampagneService campagnes;
    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Tableau de bord : statistiques et zones à risque")
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestParam(name = "jours", defaultValue = "14") int jours) {
        return dashboard.synthese(jours);
    }

    @Operation(summary = "Export CSV des signalements validés, agrégés et anonymisés")
    @GetMapping("/export/signalements.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(name = "jours", defaultValue = "14") int jours) {
        byte[] data = epidemio.exportCsv(jours).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"signalements.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }

    @Operation(summary = "Publier une alerte sanitaire")
    @PostMapping("/alertes")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public void creerAlerte(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AlerteRequest r) {
        epidemio.creerAlerte(r, Identite.uid(jwt));
    }

    @Operation(summary = "Créer une campagne (vaccination, dépistage, sensibilisation)")
    @PostMapping("/campagnes")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public CampagneDto creerCampagne(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CampagneRequest r) {
        return campagnes.creer(r, Identite.uid(jwt));
    }

    @Operation(summary = "Déclencher maintenant l'envoi des rappels SMS dus (utile en démo)")
    @PostMapping("/rappels/declencher")
    public Map<String, Integer> declencherRappels() {
        return Map.of("traites", campagnes.envoyerRappelsDus());
    }

    @Operation(summary = "Créer un compte de n'importe quel rôle (ex : modérateur médical)")
    @PostMapping("/utilisateurs")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> creerUtilisateur(@Valid @RequestBody RegisterRequest r) {
        if (utilisateurs.existsByEmail(r.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet e-mail est déjà utilisé");
        }
        Utilisateur u = utilisateurs.save(Utilisateur.builder()
                .email(r.email())
                .motDePasse(passwordEncoder.encode(r.motDePasse()))
                .nom(r.nom()).prenom(r.prenom()).telephone(r.telephone())
                .role(r.role()).build());
        return Map.of("id", u.getId(), "email", u.getEmail(), "role", u.getRole().name());
    }
}
