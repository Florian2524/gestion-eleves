package fr.afpa.backend.dto.bulletin;

import java.math.BigDecimal;

public record LigneBulletinDto(
        // Compatibilité : plus petit identifiant du groupe ; utiliser idMatiere comme identité.
        Long idEnseignement,
        Long idMatiere,
        String codeMatiere,
        String nomMatiere,
        BigDecimal coefficientMatiere,
        int nombreNotes,
        BigDecimal moyenneSur20
) {
}
