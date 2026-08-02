package fr.afpa.backend.service;

import fr.afpa.backend.dto.enseignement.EnseignementRequest;
import fr.afpa.backend.dto.enseignement.EnseignementResponse;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Enseignant;
import fr.afpa.backend.entity.Enseignement;
import fr.afpa.backend.entity.Matiere;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceInUseException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.EnseignementMapper;
import fr.afpa.backend.repository.ClasseRepository;
import fr.afpa.backend.repository.EnseignantRepository;
import fr.afpa.backend.repository.EnseignementRepository;
import fr.afpa.backend.repository.EvaluationRepository;
import fr.afpa.backend.repository.LigneBulletinRepository;
import fr.afpa.backend.repository.MatiereRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnseignementServiceTest {

    @Mock
    private EnseignementRepository enseignementRepository;

    @Mock
    private EnseignantRepository enseignantRepository;

    @Mock
    private ClasseRepository classeRepository;

    @Mock
    private MatiereRepository matiereRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private LigneBulletinRepository ligneBulletinRepository;

    @Mock
    private EnseignementMapper enseignementMapper;

    private EnseignementService enseignementService;

    private EnseignementRequest request;
    private Enseignant enseignant;
    private Classe classe;
    private Matiere matiere;
    private Enseignement enseignement;
    private EnseignementResponse response;

    @BeforeEach
    void setUp() {
        enseignementService = new EnseignementService(
                enseignementRepository,
                enseignantRepository,
                classeRepository,
                matiereRepository,
                evaluationRepository,
                ligneBulletinRepository,
                enseignementMapper
        );

        request = new EnseignementRequest(
                1L,
                2L,
                3L,
                BigDecimal.valueOf(2.5),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 6, 30),
                true
        );

        enseignant = new Enseignant(
                "Martin",
                "Sophie",
                "sophie.martin@example.com",
                "0611223344",
                "20 rue des Écoles",
                "ENS-001"
        );

        classe = new Classe(
                "6e A",
                "6e",
                "2026-2027",
                null
        );

        matiere = new Matiere(
                "MAT",
                "Mathématiques"
        );

        enseignement = new Enseignement(
                enseignant,
                classe,
                matiere,
                request.coefficientMatiere(),
                request.dateDebut(),
                request.dateFin()
        );

        enseignement.setActif(request.actif());

        response = new EnseignementResponse(
                10L,
                request.idEnseignant(),
                request.idClasse(),
                request.idMatiere(),
                request.coefficientMatiere(),
                request.dateDebut(),
                request.dateFin(),
                request.actif()
        );
    }

    @Test
    void shouldReturnAllTeachings() {
        when(enseignementRepository.findAll())
                .thenReturn(List.of(enseignement));

        when(enseignementMapper.toResponse(enseignement))
                .thenReturn(response);

        List<EnseignementResponse> result =
                enseignementService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(enseignementRepository).findAll();
        verify(enseignementMapper).toResponse(enseignement);
    }

    @Test
    void shouldReturnTeachingById() {
        when(enseignementRepository.findById(10L))
                .thenReturn(Optional.of(enseignement));

        when(enseignementMapper.toResponse(enseignement))
                .thenReturn(response);

        EnseignementResponse result =
                enseignementService.findById(10L);

        assertThat(result).isEqualTo(response);

        verify(enseignementRepository).findById(10L);
        verify(enseignementMapper).toResponse(enseignement);
    }

    @Test
    void shouldThrowExceptionWhenTeachingDoesNotExist() {
        when(enseignementRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                enseignementService.findById(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'enseignement ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(enseignementRepository).findById(99L);
        verifyNoInteractions(enseignementMapper);
    }

    @Test
    void shouldCreateTeaching() {
        when(enseignementRepository
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebut(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut()
                ))
                .thenReturn(false);

        when(enseignantRepository.findById(
                request.idEnseignant()
        )).thenReturn(Optional.of(enseignant));

        when(classeRepository.findById(
                request.idClasse()
        )).thenReturn(Optional.of(classe));

        when(matiereRepository.findById(
                request.idMatiere()
        )).thenReturn(Optional.of(matiere));

        when(enseignementMapper.toEntity(
                request,
                enseignant,
                classe,
                matiere
        )).thenReturn(enseignement);

        when(enseignementRepository.save(enseignement))
                .thenReturn(enseignement);

        when(enseignementMapper.toResponse(enseignement))
                .thenReturn(response);

        EnseignementResponse result =
                enseignementService.create(request);

        assertThat(result).isEqualTo(response);

        verify(enseignementRepository)
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebut(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut()
                );

        verify(enseignantRepository)
                .findById(request.idEnseignant());

        verify(classeRepository)
                .findById(request.idClasse());

        verify(matiereRepository)
                .findById(request.idMatiere());

        verify(enseignementMapper).toEntity(
                request,
                enseignant,
                classe,
                matiere
        );

        verify(enseignementRepository).save(enseignement);
        verify(enseignementMapper).toResponse(enseignement);
    }

    @Test
    void shouldRejectDuplicateTeachingOnCreation() {
        when(enseignementRepository
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebut(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut()
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                enseignementService.create(request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "Un enseignement existe déjà pour l'enseignant "
                                + request.idEnseignant()
                                + ", la classe "
                                + request.idClasse()
                                + ", la matière "
                                + request.idMatiere()
                                + " et la date de début "
                                + request.dateDebut()
                                + "."
                );

        verify(enseignementRepository)
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebut(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut()
                );

        verifyNoInteractions(
                enseignantRepository,
                classeRepository,
                matiereRepository,
                enseignementMapper
        );

        verify(enseignementRepository, never())
                .save(enseignement);
    }

    @Test
    void shouldRejectCreationWhenTeacherDoesNotExist() {
        when(enseignementRepository
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebut(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut()
                ))
                .thenReturn(false);

        when(enseignantRepository.findById(
                request.idEnseignant()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                enseignementService.create(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'enseignant ayant l'identifiant "
                                + request.idEnseignant()
                                + " est introuvable."
                );

        verify(enseignantRepository)
                .findById(request.idEnseignant());

        verifyNoInteractions(
                classeRepository,
                matiereRepository,
                enseignementMapper
        );

        verify(enseignementRepository, never())
                .save(enseignement);
    }

    @Test
    void shouldRejectCreationWhenClassDoesNotExist() {
        when(enseignementRepository
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebut(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut()
                ))
                .thenReturn(false);

        when(enseignantRepository.findById(
                request.idEnseignant()
        )).thenReturn(Optional.of(enseignant));

        when(classeRepository.findById(
                request.idClasse()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                enseignementService.create(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La classe ayant l'identifiant "
                                + request.idClasse()
                                + " est introuvable."
                );

        verify(enseignantRepository)
                .findById(request.idEnseignant());

        verify(classeRepository)
                .findById(request.idClasse());

        verifyNoInteractions(
                matiereRepository,
                enseignementMapper
        );

        verify(enseignementRepository, never())
                .save(enseignement);
    }

    @Test
    void shouldRejectCreationWhenSubjectDoesNotExist() {
        when(enseignementRepository
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebut(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut()
                ))
                .thenReturn(false);

        when(enseignantRepository.findById(
                request.idEnseignant()
        )).thenReturn(Optional.of(enseignant));

        when(classeRepository.findById(
                request.idClasse()
        )).thenReturn(Optional.of(classe));

        when(matiereRepository.findById(
                request.idMatiere()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                enseignementService.create(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La matière ayant l'identifiant "
                                + request.idMatiere()
                                + " est introuvable."
                );

        verify(enseignantRepository)
                .findById(request.idEnseignant());

        verify(classeRepository)
                .findById(request.idClasse());

        verify(matiereRepository)
                .findById(request.idMatiere());

        verifyNoInteractions(enseignementMapper);

        verify(enseignementRepository, never())
                .save(enseignement);
    }

    @Test
    void shouldUpdateTeaching() {
        when(enseignementRepository.findById(10L))
                .thenReturn(Optional.of(enseignement));

        when(enseignementRepository
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebutAndIdEnseignementNot(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut(),
                        10L
                ))
                .thenReturn(false);

        when(enseignantRepository.findById(
                request.idEnseignant()
        )).thenReturn(Optional.of(enseignant));

        when(classeRepository.findById(
                request.idClasse()
        )).thenReturn(Optional.of(classe));

        when(matiereRepository.findById(
                request.idMatiere()
        )).thenReturn(Optional.of(matiere));

        when(enseignementRepository.save(enseignement))
                .thenReturn(enseignement);

        when(enseignementMapper.toResponse(enseignement))
                .thenReturn(response);

        EnseignementResponse result =
                enseignementService.update(10L, request);

        assertThat(result).isEqualTo(response);

        verify(enseignementRepository).findById(10L);

        verify(enseignementRepository)
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebutAndIdEnseignementNot(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut(),
                        10L
                );

        verify(enseignementMapper).updateEntity(
                request,
                enseignant,
                classe,
                matiere,
                enseignement
        );

        verify(enseignementRepository).save(enseignement);
        verify(enseignementMapper).toResponse(enseignement);
    }

    @Test
    void shouldRejectDuplicateTeachingOnUpdate() {
        when(enseignementRepository.findById(10L))
                .thenReturn(Optional.of(enseignement));

        when(enseignementRepository
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebutAndIdEnseignementNot(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut(),
                        10L
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                enseignementService.update(10L, request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "Un autre enseignement existe déjà pour l'enseignant "
                                + request.idEnseignant()
                                + ", la classe "
                                + request.idClasse()
                                + ", la matière "
                                + request.idMatiere()
                                + " et la date de début "
                                + request.dateDebut()
                                + "."
                );

        verify(enseignementRepository).findById(10L);

        verify(enseignementRepository)
                .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebutAndIdEnseignementNot(
                        request.idEnseignant(),
                        request.idClasse(),
                        request.idMatiere(),
                        request.dateDebut(),
                        10L
                );

        verifyNoInteractions(
                enseignantRepository,
                classeRepository,
                matiereRepository
        );

        verify(enseignementMapper, never())
                .updateEntity(
                        request,
                        enseignant,
                        classe,
                        matiere,
                        enseignement
                );

        verify(enseignementRepository, never())
                .save(enseignement);
    }

    @Test
    void shouldDeleteTeaching() {
        when(enseignementRepository.findById(10L))
                .thenReturn(Optional.of(enseignement));

        when(evaluationRepository
                .existsByEnseignement_IdEnseignement(10L))
                .thenReturn(false);

        when(ligneBulletinRepository
                .existsByEnseignement_IdEnseignement(10L))
                .thenReturn(false);

        enseignementService.delete(10L);

        verify(enseignementRepository).findById(10L);

        verify(evaluationRepository)
                .existsByEnseignement_IdEnseignement(10L);

        verify(ligneBulletinRepository)
                .existsByEnseignement_IdEnseignement(10L);

        verify(enseignementRepository).delete(enseignement);
    }

    @Test
    void shouldRejectDeletionWhenTeachingDoesNotExist() {
        when(enseignementRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                enseignementService.delete(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'enseignement ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(enseignementRepository).findById(99L);

        verifyNoInteractions(
                evaluationRepository,
                ligneBulletinRepository
        );

        verify(enseignementRepository, never())
                .delete(enseignement);
    }

    @Test
    void shouldRejectDeletionWhenTeachingIsUsedByEvaluation() {
        when(enseignementRepository.findById(10L))
                .thenReturn(Optional.of(enseignement));

        when(evaluationRepository
                .existsByEnseignement_IdEnseignement(10L))
                .thenReturn(true);

        when(ligneBulletinRepository
                .existsByEnseignement_IdEnseignement(10L))
                .thenReturn(false);

        assertThatThrownBy(() ->
                enseignementService.delete(10L)
        )
                .isInstanceOf(ResourceInUseException.class)
                .hasMessage(
                        "Impossible de supprimer l'enseignement ayant l'identifiant "
                                + "10 car il est associé à au moins une évaluation ou une ligne de bulletin."
                );

        verify(evaluationRepository)
                .existsByEnseignement_IdEnseignement(10L);

        verify(ligneBulletinRepository)
                .existsByEnseignement_IdEnseignement(10L);

        verify(enseignementRepository, never())
                .delete(enseignement);
    }

    @Test
    void shouldRejectDeletionWhenTeachingIsUsedByBulletinLine() {
        when(enseignementRepository.findById(10L))
                .thenReturn(Optional.of(enseignement));

        when(evaluationRepository
                .existsByEnseignement_IdEnseignement(10L))
                .thenReturn(false);

        when(ligneBulletinRepository
                .existsByEnseignement_IdEnseignement(10L))
                .thenReturn(true);

        assertThatThrownBy(() ->
                enseignementService.delete(10L)
        )
                .isInstanceOf(ResourceInUseException.class)
                .hasMessage(
                        "Impossible de supprimer l'enseignement ayant l'identifiant "
                                + "10 car il est associé à au moins une évaluation ou une ligne de bulletin."
                );

        verify(evaluationRepository)
                .existsByEnseignement_IdEnseignement(10L);

        verify(ligneBulletinRepository)
                .existsByEnseignement_IdEnseignement(10L);

        verify(enseignementRepository, never())
                .delete(enseignement);
    }
}