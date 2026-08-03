package fr.afpa.backend.dto.bulletin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BulletinDto(
        Long idScolarite,
        Long idPeriode,
        Long idEleve,
        String matriculeEleve,
        String nomEleve,
        String prenomEleve,
        Long idClasse,
        String nomClasse,
        String niveauClasse,
        String anneeScolaire,
        String libellePeriode,
        LocalDate dateDebutPeriode,
        LocalDate dateFinPeriode,
        List<LigneBulletinDto> lignes,
        BigDecimal moyenneGenerale
) {

    public BulletinDto {
        lignes = lignes == null
                ? List.of()
                : List.copyOf(lignes);
    }
}
