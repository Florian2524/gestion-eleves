package fr.afpa.backend.dto.periode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@JsonDeserialize(using = PeriodeRequestDeserializer.class)
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
        String statut,

        String anneeScolaire,

        @JsonIgnore boolean anneeScolairePresente
) {

    /** Les appels Java explicites gardent la possibilité d'effacer l'année avec null. */
    public PeriodeRequest(String libelle, LocalDate dateDebut, LocalDate dateFin,
                          LocalDate dateDebutSaisie, LocalDate dateFinSaisie,
                          String statut, String anneeScolaire) {
        this(libelle, dateDebut, dateFin, dateDebutSaisie, dateFinSaisie,
                statut, anneeScolaire, true);
    }

    public static final String ANNEE_SCOLAIRE_MESSAGE =
            "L'année scolaire doit respecter le format AAAA-AAAA avec deux années consécutives (exemple : 2026-2027).";

    @JsonIgnore
    @AssertTrue(message = ANNEE_SCOLAIRE_MESSAGE)
    public boolean isAnneeScolaireValide() {
        return anneeScolaire == null
                || (anneeScolaire.matches("[0-9]{4}-[0-9]{4}")
                && Integer.parseInt(anneeScolaire.substring(5))
                == Integer.parseInt(anneeScolaire.substring(0, 4)) + 1);
    }

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
