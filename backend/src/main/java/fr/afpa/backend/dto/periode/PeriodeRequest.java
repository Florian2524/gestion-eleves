package fr.afpa.backend.dto.periode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PeriodeRequest(

        @NotBlank(message = "Le libellé est obligatoire.")
        @Size(
                max = 100,
                message = "Le libellé ne doit pas dépasser 100 caractères."
        )
        String libelle,

        @NotNull(message = "La date de début est obligatoire.")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire.")
        LocalDate dateFin,

        LocalDate dateDebutSaisie,

        LocalDate dateFinSaisie,

        @NotBlank(message = "Le statut est obligatoire.")
        @Size(
                max = 30,
                message = "Le statut ne doit pas dépasser 30 caractères."
        )
        String statut
) {

    @JsonIgnore
    @AssertTrue(
            message = "La date de fin doit être postérieure "
                    + "ou égale à la date de début."
    )
    public boolean isDatesPeriodeCoherentes() {
        return dateDebut == null
                || dateFin == null
                || !dateFin.isBefore(dateDebut);
    }

    @JsonIgnore
    @AssertTrue(
            message = "La date de fin de saisie doit être postérieure "
                    + "ou égale à la date de début de saisie."
    )
    public boolean isDatesSaisieCoherentes() {
        return dateDebutSaisie == null
                || dateFinSaisie == null
                || !dateFinSaisie.isBefore(dateDebutSaisie);
    }
}