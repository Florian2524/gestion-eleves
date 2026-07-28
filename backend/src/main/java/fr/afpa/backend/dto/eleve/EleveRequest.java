package fr.afpa.backend.dto.eleve;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EleveRequest(

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
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

        @NotBlank(message = "Le matricule est obligatoire.")
        @Size(
                max = 50,
                message = "Le matricule ne doit pas dépasser 50 caractères."
        )
        String matricule,

        @NotNull(message = "La date de naissance est obligatoire.")
        @Past(message = "La date de naissance doit être antérieure à aujourd'hui.")
        LocalDate dateNaissance,

        @Size(
                max = 500,
                message = "L'URL de la photo ne doit pas dépasser 500 caractères."
        )
        String photoUrl
) {
}