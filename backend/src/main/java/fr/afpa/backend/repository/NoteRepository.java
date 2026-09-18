package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository
        extends JpaRepository<Note, Long> {
    @org.springframework.data.jpa.repository.Query("select count(n) > 0 from Note n, ResponsabiliteLegale r where n.idNote = :idNote and r.eleve = n.scolarite.eleve and r.responsable.idPersonne = :idResponsable")
    boolean estLieAuResponsable(Long idNote, Long idResponsable);

    List<Note> findAllByScolarite_IdScolariteAndEvaluation_Periode_IdPeriode(
            Long idScolarite,
            Long idPeriode
    );

    List<Note> findAllByEvaluation_Enseignement_Enseignant_IdPersonne(
            Long idPersonne
    );

    boolean existsByIdNoteAndEvaluation_Enseignement_Enseignant_IdPersonne(
            Long idNote,
            Long idPersonne
    );

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
