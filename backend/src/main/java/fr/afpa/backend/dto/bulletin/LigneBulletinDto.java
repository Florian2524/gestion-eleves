package fr.afpa.backend.dto.bulletin;

import java.math.BigDecimal;

public record LigneBulletinDto(
        Long idEnseignement,
        Long idMatiere,
        String codeMatiere,
        String nomMatiere,
        BigDecimal coefficientMatiere,
        int nombreNotes,
        BigDecimal moyenneSur20
) {
}
