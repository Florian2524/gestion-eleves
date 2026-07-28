package fr.afpa.backend.dto.classe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ClasseRequest(

        @NotBlank
        @Size(max = 100)
        String nom,

        @NotBlank
        @Size(max = 50)
        String niveau,

        @NotBlank
        @Size(max = 20)
        String anneeScolaire,

        @Positive
        Long idProfesseurPrincipal
) {
}