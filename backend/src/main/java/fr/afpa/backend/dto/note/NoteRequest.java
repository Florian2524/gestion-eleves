package fr.afpa.backend.dto.note;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record NoteRequest(

        @NotNull(
                message = "L'identifiant de la scolarité est obligatoire."
        )
        @Positive(
                message = "L'identifiant de la scolarité doit être positif."
        )
        Long idScolarite,

        @NotNull(
                message = "L'identifiant de l'évaluation est obligatoire."
        )
        @Positive(
                message = "L'identifiant de l'évaluation doit être positif."
        )
        Long idEvaluation,

        @NotNull(
                message = "La valeur de la note est obligatoire."
        )
        @PositiveOrZero(
                message = "La valeur de la note doit être positive ou nulle."
        )
        @Digits(
                integer = 3,
                fraction = 2,
                message = "La valeur de la note doit contenir au maximum 3 chiffres entiers et 2 décimales."
        )
        BigDecimal valeur,

        String commentaire,

        @NotBlank(
                message = "Le statut de la note est obligatoire."
        )
        @Size(
                max = 30,
                message = "Le statut de la note ne doit pas dépasser 30 caractères."
        )
        String statutNote
) {
}
