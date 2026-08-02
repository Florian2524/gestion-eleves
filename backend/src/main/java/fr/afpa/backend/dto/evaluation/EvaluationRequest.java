package fr.afpa.backend.dto.evaluation;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EvaluationRequest(

        @NotNull(
                message = "L'identifiant de l'enseignement est obligatoire."
        )
        @Positive(
                message = "L'identifiant de l'enseignement doit être positif."
        )
        Long idEnseignement,

        @NotNull(
                message = "L'identifiant de la période est obligatoire."
        )
        @Positive(
                message = "L'identifiant de la période doit être positif."
        )
        Long idPeriode,

        @NotBlank(
                message = "Le libellé de l'évaluation est obligatoire."
        )
        @Size(
                max = 150,
                message = "Le libellé de l'évaluation ne doit pas dépasser 150 caractères."
        )
        String libelle,

        @NotNull(
                message = "La date de l'évaluation est obligatoire."
        )
        LocalDate dateEvaluation,

        @NotBlank(
                message = "Le type de l'évaluation est obligatoire."
        )
        @Size(
                max = 50,
                message = "Le type de l'évaluation ne doit pas dépasser 50 caractères."
        )
        String typeEvaluation,

        @NotNull(
                message = "Le coefficient de l'évaluation est obligatoire."
        )
        @Positive(
                message = "Le coefficient de l'évaluation doit être strictement positif."
        )
        @Digits(
                integer = 3,
                fraction = 2,
                message = "Le coefficient de l'évaluation doit contenir au maximum 3 chiffres entiers et 2 décimales."
        )
        BigDecimal coefficientEvaluation,

        @NotNull(
                message = "Le barème de l'évaluation est obligatoire."
        )
        @Positive(
                message = "Le barème de l'évaluation doit être strictement positif."
        )
        @Digits(
                integer = 3,
                fraction = 2,
                message = "Le barème de l'évaluation doit contenir au maximum 3 chiffres entiers et 2 décimales."
        )
        BigDecimal bareme
) {
}