package sn.uasz.xamle_sante.common;

import org.springframework.security.oauth2.jwt.Jwt;

public final class Identite {
    private Identite() {}

    /** Identifiant de l'utilisateur connecté, lu dans le claim "uid" du JWT. */
    public static long uid(Jwt jwt) {
        return ((Number) jwt.getClaim("uid")).longValue();
    }
}
