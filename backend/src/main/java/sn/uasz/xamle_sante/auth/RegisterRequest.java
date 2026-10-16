package sn.uasz.xamle_sante.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import sn.uasz.xamle_sante.Utilisateur.Role;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "8 caractères minimum") String motDePasse,
        @NotBlank String nom,
        @NotBlank String prenom,
        String telephone,
        @NotNull Role role
) {}