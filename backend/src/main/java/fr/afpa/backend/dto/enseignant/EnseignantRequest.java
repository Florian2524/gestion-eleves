package fr.afpa.backend.dto.enseignant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnseignantRequest(

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(
                max = 100,
                message = "Le nom ne doit pas dépasser 100 caractères."
        )
        String nom,

        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(
                max = 100,
                message = "Le prénom ne doit pas dépasser 100 caractères."
        )
        String prenom,

        @Email(message = "L'adresse e-mail n'est pas valide.")
        @Size(
                max = 255,
                message = "L'adresse e-mail ne doit pas dépasser 255 caractères."
        )
        String emailContact,

        @Size(
                max = 30,
                message = "Le numéro de téléphone ne doit pas dépasser 30 caractères."
        )
        String telephone,

        @Size(
                max = 500,
                message = "L'adresse ne doit pas dépasser 500 caractères."
        )
        String adresse,

        @NotBlank(message = "Le numéro d'employé est obligatoire.")
        @Size(
                max = 50,
                message = "Le numéro d'employé ne doit pas dépasser 50 caractères."
        )
        String numeroEmploye
) {
}