package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.evaluation.EvaluationRequest;
import fr.afpa.backend.dto.evaluation.EvaluationResponse;
import fr.afpa.backend.entity.Enseignement;
import fr.afpa.backend.entity.Evaluation;
import fr.afpa.backend.entity.Periode;
import org.springframework.stereotype.Component;

@Component
public class EvaluationMapper {

    public Evaluation toEntity(
            EvaluationRequest request,
            Enseignement enseignement,
            Periode periode
    ) {
        return new Evaluation(
                enseignement,
                periode,
                request.libelle(),
                request.dateEvaluation(),
                request.typeEvaluation(),
                request.coefficientEvaluation(),
                request.bareme()
        );
    }

    public EvaluationResponse toResponse(
            Evaluation evaluation
    ) {
        return new EvaluationResponse(
                evaluation.getIdEvaluation(),
                evaluation.getEnseignement()
                        .getIdEnseignement(),
                evaluation.getPeriode()
                        .getIdPeriode(),
                evaluation.getLibelle(),
                evaluation.getDateEvaluation(),
                evaluation.getTypeEvaluation(),
                evaluation.getCoefficientEvaluation(),
                evaluation.getBareme()
        );
    }

    public void updateEntity(
            EvaluationRequest request,
            Enseignement enseignement,
            Periode periode,
            Evaluation evaluation
    ) {
        evaluation.setEnseignement(enseignement);
        evaluation.setPeriode(periode);
        evaluation.setLibelle(request.libelle());
        evaluation.setDateEvaluation(
                request.dateEvaluation()
        );
        evaluation.setTypeEvaluation(
                request.typeEvaluation()
        );
        evaluation.setCoefficientEvaluation(
                request.coefficientEvaluation()
        );
        evaluation.setBareme(request.bareme());
    }
}