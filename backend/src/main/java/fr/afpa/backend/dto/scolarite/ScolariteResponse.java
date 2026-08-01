package fr.afpa.backend.dto.scolarite;

import java.time.LocalDate;

public record ScolariteResponse(
        Long idScolarite,
        Long idEleve,
        Long idClasse,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut
) {
}