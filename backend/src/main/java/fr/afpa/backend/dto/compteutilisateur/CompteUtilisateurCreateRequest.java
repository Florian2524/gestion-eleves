package fr.afpa.backend.dto.compteutilisateur;

import fr.afpa.backend.entity.RoleUtilisateur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CompteUtilisateurCreateRequest(

        @NotNull(message = "L'identifiant de la personne est obligatoire.")
        @Positive(message = "L'identifiant de la personne doit être strictement positif.")
        Long idPersonne,

        @NotBlank(message = "L'adresse électronique de connexion est obligatoire.")
        @Email(message = "L'adresse électronique de connexion doit être valide.")
        @Size(
                max = 255,
                message = "L'adresse électronique de connexion ne doit pas dépasser 255 caractères."
        )
        String emailConnexion,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(
                min = 8,
                max = 100,
                message = "Le mot de passe doit contenir entre 8 et 100 caractères."
        )
        String motDePasse,

        @NotNull(message = "Le rôle est obligatoire.")
        RoleUtilisateur role
) {
}
