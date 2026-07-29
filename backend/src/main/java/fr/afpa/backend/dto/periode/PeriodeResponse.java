package fr.afpa.backend.dto.periode;

import java.time.LocalDate;

public record PeriodeResponse(
        Long idPeriode,
        String libelle,
        LocalDate dateDebut,
        LocalDate dateFin,
        LocalDate dateDebutSaisie,
        LocalDate dateFinSaisie,
        String statut
) {
}