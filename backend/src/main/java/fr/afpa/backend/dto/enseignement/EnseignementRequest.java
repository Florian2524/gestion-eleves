package fr.afpa.backend.dto.enseignement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EnseignementRequest(

        @NotNull(message = "L'identifiant de l'enseignant est obligatoire.")
        @Positive(message = "L'identifiant de l'enseignant doit être positif.")
        Long idEnseignant,

        @NotNull(message = "L'identifiant de la classe est obligatoire.")
        @Positive(message = "L'identifiant de la classe doit être positif.")
        Long idClasse,

        @NotNull(message = "L'identifiant de la matière est obligatoire.")
        @Positive(message = "L'identifiant de la matière doit être positif.")
        Long idMatiere,

        @NotNull(message = "Le coefficient de la matière est obligatoire.")
        @Positive(message = "Le coefficient de la matière doit être strictement positif.")
        @Digits(
                integer = 3,
                fraction = 2,
                message = "Le coefficient de la matière doit contenir au maximum 3 chiffres entiers et 2 décimales."
        )
        BigDecimal coefficientMatiere,

        @NotNull(message = "La date de début est obligatoire.")
        LocalDate dateDebut,

        LocalDate dateFin,

        boolean actif
) {

    @JsonIgnore
    @AssertTrue(
            message = "La date de fin doit être postérieure ou égale à la date de début."
    )
    public boolean isDatesEnseignementCoherentes() {
        return dateDebut == null
                || dateFin == null
                || !dateFin.isBefore(dateDebut);
    }
}