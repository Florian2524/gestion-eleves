package fr.afpa.backend.dto.inscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record InscriptionRequest(

        @NotNull
        @Positive
        Long idEleve,

        @NotNull
        LocalDate dateInscription,

        LocalDate dateFin,

        @NotBlank
        @Size(max = 30)
        String statut
) {

    @JsonIgnore
    @AssertTrue(
            message = "La date de fin doit être postérieure "
                    + "ou égale à la date d'inscription."
    )
    public boolean isDatesInscriptionCoherentes() {
        return dateInscription == null
                || dateFin == null
                || !dateFin.isBefore(dateInscription);
    }
}