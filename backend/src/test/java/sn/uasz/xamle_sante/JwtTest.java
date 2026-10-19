package sn.uasz.xamle_sante;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import sn.uasz.xamle_sante.common.Identite;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Fabrication et vérification des jetons JWT (HMAC-SHA256), comme dans SecurityConfig. */
class JwtTest {

    private static SecretKey cle(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static final SecretKey CLE = cle("cle-de-test-xamle-sante-uasz-0123456789-abcdef");

    private static Jwt fabriquer(Instant debut, Instant fin) {
        NimbusJwtEncoder encodeur = new NimbusJwtEncoder(new ImmutableSecret<>(CLE));
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("patient@xamle.sn")
                .claim("role", "PATIENT").claim("uid", 42L).issuedAt(debut).expiresAt(fin).build();
        return encodeur.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
    }

    private static JwtDecoder decodeur(SecretKey k) {
        return NimbusJwtDecoder.withSecretKey(k).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    void unJetonValideSeRelitAvecSesInformations() {
        Jwt jeton = fabriquer(Instant.now(), Instant.now().plusSeconds(3600));
        Jwt relu = decodeur(CLE).decode(jeton.getTokenValue());
        assertEquals("patient@xamle.sn", relu.getSubject());
        assertEquals("PATIENT", relu.getClaimAsString("role"));
        assertEquals(42L, Identite.uid(relu));
    }

    @Test
    void unJetonSigneAvecUneAutreCleEstRefuse() {
        Jwt jeton = fabriquer(Instant.now(), Instant.now().plusSeconds(3600));
        JwtDecoder autre = decodeur(cle("une-autre-cle-secrete-0123456789-abcdefghij"));
        assertThrows(JwtException.class, () -> autre.decode(jeton.getTokenValue()));
    }

    @Test
    void unJetonExpireEstRefuse() {
        Jwt jeton = fabriquer(Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
        assertThrows(JwtException.class, () -> decodeur(CLE).decode(jeton.getTokenValue()));
    }

    @Test
    void unJetonModifieEstRefuse() {
        String valeur = fabriquer(Instant.now(), Instant.now().plusSeconds(3600)).getTokenValue();
        String altere = valeur.substring(0, valeur.length() - 3) + (valeur.endsWith("abc") ? "xyz" : "abc");
        assertThrows(JwtException.class, () -> decodeur(CLE).decode(altere));
    }
}
