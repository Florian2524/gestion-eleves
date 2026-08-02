package fr.afpa.backend.dto.inscription;

import java.time.LocalDate;

public record InscriptionResponse(
        Long idInscription,
        Long idEleve,
        LocalDate dateInscription,
        LocalDate dateFin,
        String statut
) {
}