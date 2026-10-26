package fr.afpa.backend.dto.ponderer;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PondererRequest(
        @NotNull @Positive Long idClasse,
        @NotNull @Positive Long idMatiere,
        @NotNull @Positive @Digits(integer = 3, fraction = 2) BigDecimal coefficientMatiere
) { }
