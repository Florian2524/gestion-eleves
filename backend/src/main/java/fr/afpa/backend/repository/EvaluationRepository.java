package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRepository
        extends JpaRepository<Evaluation, Long> {

    boolean existsByEnseignement_IdEnseignement(
            Long idEnseignement
    );
}