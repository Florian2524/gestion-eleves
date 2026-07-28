package fr.afpa.backend.dto.matiere;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MatiereRequest(

        @NotBlank
        @Size(max = 30)
        String code,

        @NotBlank
        @Size(max = 100)
        String nom
) {
}