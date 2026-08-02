package fr.afpa.backend.service;

import fr.afpa.backend.dto.evaluation.EvaluationRequest;
import fr.afpa.backend.dto.evaluation.EvaluationResponse;
import fr.afpa.backend.entity.Enseignement;
import fr.afpa.backend.entity.Evaluation;
import fr.afpa.backend.entity.Periode;
import fr.afpa.backend.exception.ResourceInUseException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.EvaluationMapper;
import fr.afpa.backend.repository.EnseignementRepository;
import fr.afpa.backend.repository.EvaluationRepository;
import fr.afpa.backend.repository.NoteRepository;
import fr.afpa.backend.repository.PeriodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EnseignementRepository enseignementRepository;
    private final PeriodeRepository periodeRepository;
    private final NoteRepository noteRepository;
    private final EvaluationMapper evaluationMapper;

    public EvaluationService(
            EvaluationRepository evaluationRepository,
            EnseignementRepository enseignementRepository,
            PeriodeRepository periodeRepository,
            NoteRepository noteRepository,
            EvaluationMapper evaluationMapper
    ) {
        this.evaluationRepository = evaluationRepository;
        this.enseignementRepository = enseignementRepository;
        this.periodeRepository = periodeRepository;
        this.noteRepository = noteRepository;
        this.evaluationMapper = evaluationMapper;
    }

    public List<EvaluationResponse> findAll() {
        return evaluationRepository.findAll()
                .stream()
                .map(evaluationMapper::toResponse)
                .toList();
    }

    public EvaluationResponse findById(Long id) {
        return evaluationMapper.toResponse(
                findEntityById(id)
        );
    }

    @Transactional
    public EvaluationResponse create(
            EvaluationRequest request
    ) {
        Enseignement enseignement =
                findEnseignementById(
                        request.idEnseignement()
                );

        Periode periode =
                findPeriodeById(request.idPeriode());

        Evaluation evaluation =
                evaluationMapper.toEntity(
                        request,
                        enseignement,
                        periode
                );

        Evaluation savedEvaluation =
                evaluationRepository.save(evaluation);

        return evaluationMapper.toResponse(
                savedEvaluation
        );
    }

    @Transactional
    public EvaluationResponse update(
            Long id,
            EvaluationRequest request
    ) {
        Evaluation evaluation =
                findEntityById(id);

        Enseignement enseignement =
                findEnseignementById(
                        request.idEnseignement()
                );

        Periode periode =
                findPeriodeById(request.idPeriode());

        evaluationMapper.updateEntity(
                request,
                enseignement,
                periode,
                evaluation
        );

        Evaluation updatedEvaluation =
                evaluationRepository.save(evaluation);

        return evaluationMapper.toResponse(
                updatedEvaluation
        );
    }

    @Transactional
    public void delete(Long id) {
        Evaluation evaluation =
                findEntityById(id);

        boolean usedByNote =
                noteRepository
                        .existsByEvaluation_IdEvaluation(id);

        if (usedByNote) {
            throw new ResourceInUseException(
                    "Impossible de supprimer l'évaluation ayant l'identifiant "
                            + id
                            + " car elle est associée à au moins une note."
            );
        }

        evaluationRepository.delete(evaluation);
    }

    private Evaluation findEntityById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "L'évaluation ayant l'identifiant "
                                        + id
                                        + " est introuvable."
                        )
                );
    }

    private Enseignement findEnseignementById(
            Long idEnseignement
    ) {
        return enseignementRepository
                .findById(idEnseignement)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "L'enseignement ayant l'identifiant "
                                        + idEnseignement
                                        + " est introuvable."
                        )
                );
    }

    private Periode findPeriodeById(Long idPeriode) {
        return periodeRepository.findById(idPeriode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La période ayant l'identifiant "
                                        + idPeriode
                                        + " est introuvable."
                        )
                );
    }
}