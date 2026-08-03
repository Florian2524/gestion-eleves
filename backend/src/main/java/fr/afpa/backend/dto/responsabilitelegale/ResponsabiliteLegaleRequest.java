package fr.afpa.backend.dto.responsabilitelegale;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ResponsabiliteLegaleRequest(

        @NotNull(message = "L'identifiant du responsable est obligatoire.")
        @Positive(message = "L'identifiant du responsable doit être strictement positif.")
        Long idResponsable,

        @NotNull(message = "L'identifiant de l'élève est obligatoire.")
        @Positive(message = "L'identifiant de l'élève doit être strictement positif.")
        Long idEleve
) {
}
