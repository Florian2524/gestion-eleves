package fr.afpa.backend.dto.scolarite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ScolariteRequest(

        @NotNull
        @Positive
        Long idEleve,

        @NotNull
        @Positive
        Long idClasse,

        @NotNull
        LocalDate dateDebut,

        LocalDate dateFin,

        @NotBlank
        @Size(max = 30)
        String statut
) {

    @JsonIgnore
    @AssertTrue(
            message = "La date de fin doit être postérieure "
                    + "ou égale à la date de début."
    )
    public boolean isDatesScolariteCoherentes() {
        return dateDebut == null
                || dateFin == null
                || !dateFin.isBefore(dateDebut);
    }
}