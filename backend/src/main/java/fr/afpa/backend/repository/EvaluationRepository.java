package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository
        extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findAllByEnseignement_Enseignant_IdPersonne(
            Long idPersonne
    );

    boolean existsByIdEvaluationAndEnseignement_Enseignant_IdPersonne(
            Long idEvaluation,
            Long idPersonne
    );

    boolean existsByEnseignement_IdEnseignement(
            Long idEnseignement
    );
}
