package fr.afpa.backend.dto.note;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record NoteResponse(
        Long idNote,
        Long idScolarite,
        Long idEvaluation,
        BigDecimal valeur,
        String commentaire,
        String statutNote,
        OffsetDateTime dateSaisie
) {
}
