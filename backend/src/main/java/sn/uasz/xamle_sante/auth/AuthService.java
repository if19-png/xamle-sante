package sn.uasz.xamle_sante.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle_sante.Utilisateur.Role;
import sn.uasz.xamle_sante.Utilisateur.Utilisateur;
import sn.uasz.xamle_sante.Utilisateur.UtilisateurRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    public AuthResponse register(RegisterRequest req) {
        // Seuls PATIENT et PROFESSIONNEL peuvent s'inscrire eux-mêmes
        if (req.role() != Role.PATIENT && req.role() != Role.PROFESSIONNEL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce rôle ne peut pas être choisi à l'inscription");
        }
        if (utilisateurRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet e-mail est déjà utilisé");
        }
        Utilisateur u = Utilisateur.builder()
                .email(req.email())
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .nom(req.nom())
                .prenom(req.prenom())
                .telephone(req.telephone())
                .role(req.role())
                .build();
        utilisateurRepository.save(u);
        return genererToken(u);
    }

    public AuthResponse login(LoginRequest req) {
        Utilisateur u = utilisateurRepository.findByEmail(req.email())
                .filter(Utilisateur::isActif)
                .filter(x -> passwordEncoder.matches(req.motDePasse(), x.getMotDePasse()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Identifiants invalides"));
        return genererToken(u);
    }

    private AuthResponse genererToken(Utilisateur u) {
        Instant maintenant = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(u.getEmail())
                .issuedAt(maintenant)
                .expiresAt(maintenant.plus(expirationMinutes, ChronoUnit.MINUTES))
                .claim("role", u.getRole().name())
                .claim("uid", u.getId())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", expirationMinutes * 60, u.getRole().name());
    }
}