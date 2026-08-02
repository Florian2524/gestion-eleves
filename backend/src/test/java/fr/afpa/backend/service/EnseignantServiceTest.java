package fr.afpa.backend.service;

import fr.afpa.backend.dto.enseignant.EnseignantRequest;
import fr.afpa.backend.dto.enseignant.EnseignantResponse;
import fr.afpa.backend.entity.Enseignant;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceInUseException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.EnseignantMapper;
import fr.afpa.backend.repository.EnseignantRepository;
import fr.afpa.backend.repository.EnseignementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnseignantServiceTest {

    @Mock
    private EnseignantRepository enseignantRepository;

    @Mock
    private EnseignementRepository enseignementRepository;

    @Mock
    private EnseignantMapper enseignantMapper;

    private EnseignantService enseignantService;

    private EnseignantRequest request;
    private Enseignant enseignant;
    private EnseignantResponse response;

    @BeforeEach
    void setUp() {
        enseignantService = new EnseignantService(
                enseignantRepository,
                enseignementRepository,
                enseignantMapper
        );

        request = new EnseignantRequest(
                "Martin",
                "Sophie",
                "sophie.martin@example.com",
                "0611223344",
                "20 rue des Écoles",
                "ENS-001"
        );

        enseignant = new Enseignant(
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.numeroEmploye()
        );

        response = new EnseignantResponse(
                1L,
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.numeroEmploye()
        );
    }

    @Test
    void shouldReturnAllTeachers() {
        when(enseignantRepository.findAll())
                .thenReturn(List.of(enseignant));

        when(enseignantMapper.toResponse(enseignant))
                .thenReturn(response);

        List<EnseignantResponse> result =
                enseignantService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(enseignantRepository).findAll();
        verify(enseignantMapper).toResponse(enseignant);
    }

    @Test
    void shouldReturnTeacherById() {
        when(enseignantRepository.findById(1L))
                .thenReturn(Optional.of(enseignant));

        when(enseignantMapper.toResponse(enseignant))
                .thenReturn(response);

        EnseignantResponse result =
                enseignantService.findById(1L);

        assertThat(result).isEqualTo(response);

        verify(enseignantRepository).findById(1L);
        verify(enseignantMapper).toResponse(enseignant);
    }

    @Test
    void shouldThrowExceptionWhenTeacherDoesNotExist() {
        when(enseignantRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                enseignantService.findById(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'enseignant ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(enseignantRepository).findById(99L);
        verifyNoInteractions(enseignantMapper);
    }

    @Test
    void shouldCreateTeacher() {
        when(enseignantRepository.existsByNumeroEmploye(
                request.numeroEmploye()
        )).thenReturn(false);

        when(enseignantMapper.toEntity(request))
                .thenReturn(enseignant);

        when(enseignantRepository.save(enseignant))
                .thenReturn(enseignant);

        when(enseignantMapper.toResponse(enseignant))
                .thenReturn(response);

        EnseignantResponse result =
                enseignantService.create(request);

        assertThat(result).isEqualTo(response);

        verify(enseignantRepository)
                .existsByNumeroEmploye(
                        request.numeroEmploye()
                );

        verify(enseignantMapper).toEntity(request);
        verify(enseignantRepository).save(enseignant);
        verify(enseignantMapper).toResponse(enseignant);
    }

    @Test
    void shouldRejectDuplicateEmployeeNumberOnCreation() {
        when(enseignantRepository.existsByNumeroEmploye(
                request.numeroEmploye()
        )).thenReturn(true);

        assertThatThrownBy(() ->
                enseignantService.create(request)
        )
                .isInstanceOf(
                        DuplicateResourceException.class
                )
                .hasMessage(
                        "Un enseignant possède déjà le numéro d'employé "
                                + request.numeroEmploye()
                                + "."
                );

        verify(enseignantRepository)
                .existsByNumeroEmploye(
                        request.numeroEmploye()
                );

        verify(enseignantRepository, never())
                .save(enseignant);

        verifyNoInteractions(enseignantMapper);
    }

    @Test
    void shouldUpdateTeacher() {
        when(enseignantRepository.findById(1L))
                .thenReturn(Optional.of(enseignant));

        when(enseignantRepository
                .existsByNumeroEmployeAndIdPersonneNot(
                        request.numeroEmploye(),
                        1L
                ))
                .thenReturn(false);

        when(enseignantRepository.save(enseignant))
                .thenReturn(enseignant);

        when(enseignantMapper.toResponse(enseignant))
                .thenReturn(response);

        EnseignantResponse result =
                enseignantService.update(1L, request);

        assertThat(result).isEqualTo(response);

        verify(enseignantRepository).findById(1L);

        verify(enseignantRepository)
                .existsByNumeroEmployeAndIdPersonneNot(
                        request.numeroEmploye(),
                        1L
                );

        verify(enseignantMapper)
                .updateEntity(request, enseignant);

        verify(enseignantRepository).save(enseignant);
        verify(enseignantMapper).toResponse(enseignant);
    }

    @Test
    void shouldRejectDuplicateEmployeeNumberOnUpdate() {
        when(enseignantRepository.findById(1L))
                .thenReturn(Optional.of(enseignant));

        when(enseignantRepository
                .existsByNumeroEmployeAndIdPersonneNot(
                        request.numeroEmploye(),
                        1L
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                enseignantService.update(1L, request)
        )
                .isInstanceOf(
                        DuplicateResourceException.class
                )
                .hasMessage(
                        "Un autre enseignant possède déjà le numéro d'employé "
                                + request.numeroEmploye()
                                + "."
                );

        verify(enseignantRepository).findById(1L);

        verify(enseignantRepository)
                .existsByNumeroEmployeAndIdPersonneNot(
                        request.numeroEmploye(),
                        1L
                );

        verify(enseignantMapper, never())
                .updateEntity(request, enseignant);

        verify(enseignantRepository, never())
                .save(enseignant);
    }

    @Test
    void shouldDeleteTeacher() {
        when(enseignantRepository.findById(1L))
                .thenReturn(Optional.of(enseignant));

        when(enseignementRepository
                .existsByEnseignant_IdPersonne(1L))
                .thenReturn(false);

        enseignantService.delete(1L);

        verify(enseignantRepository).findById(1L);

        verify(enseignementRepository)
                .existsByEnseignant_IdPersonne(1L);

        verify(enseignantRepository).delete(enseignant);
    }

    @Test
    void shouldRejectDeletionWhenTeacherDoesNotExist() {
        when(enseignantRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                enseignantService.delete(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'enseignant ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(enseignantRepository).findById(99L);
        verifyNoInteractions(enseignementRepository);

        verify(enseignantRepository, never())
                .delete(enseignant);
    }

    @Test
    void shouldRejectDeletionWhenTeacherIsUsed() {
        when(enseignantRepository.findById(1L))
                .thenReturn(Optional.of(enseignant));

        when(enseignementRepository
                .existsByEnseignant_IdPersonne(1L))
                .thenReturn(true);

        assertThatThrownBy(() ->
                enseignantService.delete(1L)
        )
                .isInstanceOf(ResourceInUseException.class)
                .hasMessage(
                        "Impossible de supprimer l'enseignant ayant l'identifiant "
                                + "1 car il est associé à au moins un enseignement."
                );

        verify(enseignantRepository).findById(1L);

        verify(enseignementRepository)
                .existsByEnseignant_IdPersonne(1L);

        verify(enseignantRepository, never())
                .delete(enseignant);
    }
}