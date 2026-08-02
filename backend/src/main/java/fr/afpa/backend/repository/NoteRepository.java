package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository
        extends JpaRepository<Note, Long> {

    boolean existsByEvaluation_IdEvaluation(
            Long idEvaluation
    );

    boolean existsByScolarite_IdScolariteAndEvaluation_IdEvaluation(
            Long idScolarite,
            Long idEvaluation
    );

    boolean existsByScolarite_IdScolariteAndEvaluation_IdEvaluationAndIdNoteNot(
            Long idScolarite,
            Long idEvaluation,
            Long idNote
    );
}
