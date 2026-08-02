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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private EnseignementRepository enseignementRepository;

    @Mock
    private PeriodeRepository periodeRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private EvaluationMapper evaluationMapper;

    @Mock
    private Enseignement enseignement;

    @Mock
    private Periode periode;

    private EvaluationService evaluationService;

    private EvaluationRequest request;
    private Evaluation evaluation;
    private EvaluationResponse response;

    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationService(
                evaluationRepository,
                enseignementRepository,
                periodeRepository,
                noteRepository,
                evaluationMapper
        );

        request = new EvaluationRequest(
                10L,
                2L,
                "Contrôle de mathématiques",
                LocalDate.of(2026, 10, 15),
                "CONTROLE",
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(20)
        );

        evaluation = new Evaluation(
                enseignement,
                periode,
                request.libelle(),
                request.dateEvaluation(),
                request.typeEvaluation(),
                request.coefficientEvaluation(),
                request.bareme()
        );

        response = new EvaluationResponse(
                100L,
                request.idEnseignement(),
                request.idPeriode(),
                request.libelle(),
                request.dateEvaluation(),
                request.typeEvaluation(),
                request.coefficientEvaluation(),
                request.bareme()
        );
    }

    @Test
    void shouldReturnAllEvaluations() {
        when(evaluationRepository.findAll())
                .thenReturn(List.of(evaluation));

        when(evaluationMapper.toResponse(evaluation))
                .thenReturn(response);

        List<EvaluationResponse> result =
                evaluationService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(evaluationRepository).findAll();
        verify(evaluationMapper).toResponse(evaluation);
    }

    @Test
    void shouldReturnEvaluationById() {
        when(evaluationRepository.findById(100L))
                .thenReturn(Optional.of(evaluation));

        when(evaluationMapper.toResponse(evaluation))
                .thenReturn(response);

        EvaluationResponse result =
                evaluationService.findById(100L);

        assertThat(result).isEqualTo(response);

        verify(evaluationRepository).findById(100L);
        verify(evaluationMapper).toResponse(evaluation);
    }

    @Test
    void shouldThrowExceptionWhenEvaluationDoesNotExist() {
        when(evaluationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                evaluationService.findById(999L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'évaluation ayant l'identifiant "
                                + "999 est introuvable."
                );

        verify(evaluationRepository).findById(999L);
        verifyNoInteractions(evaluationMapper);
    }

    @Test
    void shouldCreateEvaluation() {
        when(enseignementRepository.findById(
                request.idEnseignement()
        )).thenReturn(Optional.of(enseignement));

        when(periodeRepository.findById(
                request.idPeriode()
        )).thenReturn(Optional.of(periode));

        when(evaluationMapper.toEntity(
                request,
                enseignement,
                periode
        )).thenReturn(evaluation);

        when(evaluationRepository.save(evaluation))
                .thenReturn(evaluation);

        when(evaluationMapper.toResponse(evaluation))
                .thenReturn(response);

        EvaluationResponse result =
                evaluationService.create(request);

        assertThat(result).isEqualTo(response);

        verify(enseignementRepository)
                .findById(request.idEnseignement());

        verify(periodeRepository)
                .findById(request.idPeriode());

        verify(evaluationMapper).toEntity(
                request,
                enseignement,
                periode
        );

        verify(evaluationRepository).save(evaluation);
        verify(evaluationMapper).toResponse(evaluation);
    }

    @Test
    void shouldRejectCreationWhenTeachingDoesNotExist() {
        when(enseignementRepository.findById(
                request.idEnseignement()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                evaluationService.create(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'enseignement ayant l'identifiant "
                                + request.idEnseignement()
                                + " est introuvable."
                );

        verify(enseignementRepository)
                .findById(request.idEnseignement());

        verifyNoInteractions(
                periodeRepository,
                evaluationMapper
        );

        verify(evaluationRepository, never())
                .save(any(Evaluation.class));
    }

    @Test
    void shouldRejectCreationWhenPeriodDoesNotExist() {
        when(enseignementRepository.findById(
                request.idEnseignement()
        )).thenReturn(Optional.of(enseignement));

        when(periodeRepository.findById(
                request.idPeriode()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                evaluationService.create(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La période ayant l'identifiant "
                                + request.idPeriode()
                                + " est introuvable."
                );

        verify(enseignementRepository)
                .findById(request.idEnseignement());

        verify(periodeRepository)
                .findById(request.idPeriode());

        verifyNoInteractions(evaluationMapper);

        verify(evaluationRepository, never())
                .save(any(Evaluation.class));
    }

    @Test
    void shouldUpdateEvaluation() {
        when(evaluationRepository.findById(100L))
                .thenReturn(Optional.of(evaluation));

        when(enseignementRepository.findById(
                request.idEnseignement()
        )).thenReturn(Optional.of(enseignement));

        when(periodeRepository.findById(
                request.idPeriode()
        )).thenReturn(Optional.of(periode));

        when(evaluationRepository.save(evaluation))
                .thenReturn(evaluation);

        when(evaluationMapper.toResponse(evaluation))
                .thenReturn(response);

        EvaluationResponse result =
                evaluationService.update(
                        100L,
                        request
                );

        assertThat(result).isEqualTo(response);

        verify(evaluationRepository).findById(100L);

        verify(enseignementRepository)
                .findById(request.idEnseignement());

        verify(periodeRepository)
                .findById(request.idPeriode());

        verify(evaluationMapper).updateEntity(
                request,
                enseignement,
                periode,
                evaluation
        );

        verify(evaluationRepository).save(evaluation);
        verify(evaluationMapper).toResponse(evaluation);
    }

    @Test
    void shouldRejectUpdateWhenEvaluationDoesNotExist() {
        when(evaluationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                evaluationService.update(
                        999L,
                        request
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'évaluation ayant l'identifiant "
                                + "999 est introuvable."
                );

        verify(evaluationRepository).findById(999L);

        verifyNoInteractions(
                enseignementRepository,
                periodeRepository,
                evaluationMapper
        );

        verify(evaluationRepository, never())
                .save(any(Evaluation.class));
    }

    @Test
    void shouldRejectUpdateWhenTeachingDoesNotExist() {
        when(evaluationRepository.findById(100L))
                .thenReturn(Optional.of(evaluation));

        when(enseignementRepository.findById(
                request.idEnseignement()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                evaluationService.update(
                        100L,
                        request
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'enseignement ayant l'identifiant "
                                + request.idEnseignement()
                                + " est introuvable."
                );

        verify(evaluationRepository).findById(100L);

        verify(enseignementRepository)
                .findById(request.idEnseignement());

        verifyNoInteractions(
                periodeRepository,
                evaluationMapper
        );

        verify(evaluationRepository, never())
                .save(any(Evaluation.class));
    }

    @Test
    void shouldRejectUpdateWhenPeriodDoesNotExist() {
        when(evaluationRepository.findById(100L))
                .thenReturn(Optional.of(evaluation));

        when(enseignementRepository.findById(
                request.idEnseignement()
        )).thenReturn(Optional.of(enseignement));

        when(periodeRepository.findById(
                request.idPeriode()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                evaluationService.update(
                        100L,
                        request
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La période ayant l'identifiant "
                                + request.idPeriode()
                                + " est introuvable."
                );

        verify(evaluationRepository).findById(100L);

        verify(enseignementRepository)
                .findById(request.idEnseignement());

        verify(periodeRepository)
                .findById(request.idPeriode());

        verifyNoInteractions(evaluationMapper);

        verify(evaluationRepository, never())
                .save(any(Evaluation.class));
    }

    @Test
    void shouldDeleteEvaluation() {
        when(evaluationRepository.findById(100L))
                .thenReturn(Optional.of(evaluation));

        when(noteRepository
                .existsByEvaluation_IdEvaluation(100L))
                .thenReturn(false);

        evaluationService.delete(100L);

        verify(evaluationRepository).findById(100L);

        verify(noteRepository)
                .existsByEvaluation_IdEvaluation(100L);

        verify(evaluationRepository).delete(evaluation);
    }

    @Test
    void shouldRejectDeletionWhenEvaluationDoesNotExist() {
        when(evaluationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                evaluationService.delete(999L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'évaluation ayant l'identifiant "
                                + "999 est introuvable."
                );

        verify(evaluationRepository).findById(999L);
        verifyNoInteractions(noteRepository);

        verify(evaluationRepository, never())
                .delete(any(Evaluation.class));
    }

    @Test
    void shouldRejectDeletionWhenEvaluationIsUsedByNote() {
        when(evaluationRepository.findById(100L))
                .thenReturn(Optional.of(evaluation));

        when(noteRepository
                .existsByEvaluation_IdEvaluation(100L))
                .thenReturn(true);

        assertThatThrownBy(() ->
                evaluationService.delete(100L)
        )
                .isInstanceOf(ResourceInUseException.class)
                .hasMessage(
                        "Impossible de supprimer l'évaluation ayant l'identifiant "
                                + "100 car elle est associée à au moins une note."
                );

        verify(evaluationRepository).findById(100L);

        verify(noteRepository)
                .existsByEvaluation_IdEvaluation(100L);

        verify(evaluationRepository, never())
                .delete(evaluation);
    }
}