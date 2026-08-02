package fr.afpa.backend.dto.evaluation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EvaluationResponse(
        Long idEvaluation,
        Long idEnseignement,
        Long idPeriode,
        String libelle,
        LocalDate dateEvaluation,
        String typeEvaluation,
        BigDecimal coefficientEvaluation,
        BigDecimal bareme
) {
}