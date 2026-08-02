package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.note.NoteRequest;
import fr.afpa.backend.dto.note.NoteResponse;
import fr.afpa.backend.entity.Evaluation;
import fr.afpa.backend.entity.Note;
import fr.afpa.backend.entity.Scolarite;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    public Note toEntity(
            NoteRequest request,
            Scolarite scolarite,
            Evaluation evaluation
    ) {
        return new Note(
                scolarite,
                evaluation,
                request.valeur(),
                request.commentaire(),
                request.statutNote()
        );
    }

    public NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getIdNote(),
                note.getScolarite().getIdScolarite(),
                note.getEvaluation().getIdEvaluation(),
                note.getValeur(),
                note.getCommentaire(),
                note.getStatutNote(),
                note.getDateSaisie()
        );
    }

    public void updateEntity(
            NoteRequest request,
            Scolarite scolarite,
            Evaluation evaluation,
            Note note
    ) {
        note.setScolarite(scolarite);
        note.setEvaluation(evaluation);
        note.setValeur(request.valeur());
        note.setCommentaire(request.commentaire());
        note.setStatutNote(request.statutNote());
    }
}
