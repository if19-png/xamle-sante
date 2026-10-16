package sn.uasz.xamle_sante.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String motDePasse
) {}