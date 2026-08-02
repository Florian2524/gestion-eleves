package fr.afpa.backend.dto.enseignement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EnseignementResponse(
        Long idEnseignement,
        Long idEnseignant,
        Long idClasse,
        Long idMatiere,
        BigDecimal coefficientMatiere,
        LocalDate dateDebut,
        LocalDate dateFin,
        boolean actif
) {
}