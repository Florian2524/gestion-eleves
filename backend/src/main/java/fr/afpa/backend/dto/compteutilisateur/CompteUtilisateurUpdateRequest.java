package fr.afpa.backend.dto.compteutilisateur;

import fr.afpa.backend.entity.RoleUtilisateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompteUtilisateurUpdateRequest(

        @NotBlank(message = "L'adresse électronique de connexion est obligatoire.")
        @Email(message = "L'adresse électronique de connexion doit être valide.")
        @Size(
                max = 255,
                message = "L'adresse électronique de connexion ne doit pas dépasser 255 caractères."
        )
        String emailConnexion,

        @Size(
                min = 8,
                max = 100,
                message = "Le nouveau mot de passe doit contenir entre 8 et 100 caractères."
        )
        String nouveauMotDePasse,

        @NotNull(message = "Le rôle est obligatoire.")
        RoleUtilisateur role,

        @NotNull(message = "L'état actif du compte est obligatoire.")
        Boolean actif
) {
}
