package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository
        extends JpaRepository<Note, Long> {

    boolean existsByEvaluation_IdEvaluation(
            Long idEvaluation
    );
}