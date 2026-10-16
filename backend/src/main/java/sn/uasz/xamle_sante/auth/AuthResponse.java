package sn.uasz.xamle_sante.auth;

public record AuthResponse(String token, String type, long expireDansSecondes, String role) {}