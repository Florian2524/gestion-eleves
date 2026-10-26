package fr.afpa.backend.dto.enseignement;

import java.math.BigDecimal;
import java.time.LocalDate;

/** coefficientMatiere reste compatible : initialisation/vérification du couple PONDERER. */
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