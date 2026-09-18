package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository
        extends JpaRepository<Evaluation, Long> {
    @org.springframework.data.jpa.repository.Query("select count(e) > 0 from Evaluation e, Scolarite s, ResponsabiliteLegale r where e.idEvaluation = :idEvaluation and s.classe = e.enseignement.classe and r.eleve = s.eleve and r.responsable.idPersonne = :idResponsable")
    boolean concerneResponsable(Long idEvaluation, Long idResponsable);

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
