package fr.afpa.backend.service;

import fr.afpa.backend.dto.scolarite.ScolariteRequest;
import fr.afpa.backend.dto.scolarite.ScolariteResponse;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.Scolarite;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.ScolariteMapper;
import fr.afpa.backend.repository.ClasseRepository;
import fr.afpa.backend.repository.EleveRepository;
import fr.afpa.backend.repository.ScolariteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class ScolariteServiceTest {

    @Mock
    private ScolariteRepository scolariteRepository;

    @Mock
    private EleveRepository eleveRepository;

    @Mock
    private ClasseRepository classeRepository;

    @Mock
    private ScolariteMapper scolariteMapper;

    private ScolariteService scolariteService;

    private ScolariteRequest request;
    private Eleve eleve;
    private Classe classe;
    private Scolarite scolarite;
    private ScolariteResponse response;

    @BeforeEach
    void setUp() {
        scolariteService = new ScolariteService(
                scolariteRepository,
                eleveRepository,
                classeRepository,
                scolariteMapper
        );

        request = new ScolariteRequest(
                1L,
                2L,
                LocalDate.of(2026, 9, 1),
                null,
                "EN_COURS"
        );

        eleve = new Eleve(
                "Martin",
                "Léa",
                "lea.martin@example.com",
                "0611223344",
                "12 rue des Écoles",
                "ELV-001",
                LocalDate.of(2014, 3, 15),
                null
        );

        classe = new Classe(
                "6A",
                "Sixième",
                "2026-2027",
                null
        );

        scolarite = new Scolarite(
                eleve,
                classe,
                request.dateDebut(),
                request.dateFin(),
                request.statut()
        );

        response = new ScolariteResponse(
                3L,
                request.idEleve(),
                request.idClasse(),
                request.dateDebut(),
                request.dateFin(),
                request.statut()
        );
    }

    @Test
    void shouldReturnAllScolarites() {
        when(scolariteRepository.findAll())
                .thenReturn(List.of(scolarite));

        when(scolariteMapper.toResponse(scolarite))
                .thenReturn(response);

        List<ScolariteResponse> result =
                scolariteService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(scolariteRepository).findAll();
        verify(scolariteMapper).toResponse(scolarite);
    }

    @Test
    void shouldReturnScolariteById() {
        when(scolariteRepository.findById(3L))
                .thenReturn(Optional.of(scolarite));

        when(scolariteMapper.toResponse(scolarite))
                .thenReturn(response);

        ScolariteResponse result =
                scolariteService.findById(3L);

        assertThat(result).isEqualTo(response);

        verify(scolariteRepository).findById(3L);
        verify(scolariteMapper).toResponse(scolarite);
    }

    @Test
    void shouldThrowExceptionWhenScolariteDoesNotExist() {
        when(scolariteRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                scolariteService.findById(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La scolarité ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(scolariteRepository).findById(99L);
        verifyNoInteractions(scolariteMapper);
    }

    @Test
    void shouldCreateScolarite() {
        when(scolariteRepository
                .existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebut(
                        request.idEleve(),
                        request.idClasse(),
                        request.dateDebut()
                ))
                .thenReturn(false);

        when(eleveRepository.findById(request.idEleve()))
                .thenReturn(Optional.of(eleve));

        when(classeRepository.findById(request.idClasse()))
                .thenReturn(Optional.of(classe));

        when(scolariteMapper.toEntity(
                request,
                eleve,
                classe
        )).thenReturn(scolarite);

        when(scolariteRepository.save(scolarite))
                .thenReturn(scolarite);

        when(scolariteMapper.toResponse(scolarite))
                .thenReturn(response);

        ScolariteResponse result =
                scolariteService.create(request);

        assertThat(result).isEqualTo(response);

        verify(eleveRepository).findById(1L);
        verify(classeRepository).findById(2L);

        verify(scolariteMapper).toEntity(
                request,
                eleve,
                classe
        );

        verify(scolariteRepository).save(scolarite);
        verify(scolariteMapper).toResponse(scolarite);
    }

    @Test
    void shouldRejectDuplicateScolariteOnCreation() {
        when(scolariteRepository
                .existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebut(
                        request.idEleve(),
                        request.idClasse(),
                        request.dateDebut()
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                scolariteService.create(request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("1")
                .hasMessageContaining("2")
                .hasMessageContaining("2026-09-01");

        verifyNoInteractions(
                eleveRepository,
                classeRepository,
                scolariteMapper
        );

        verify(scolariteRepository, never())
                .save(any(Scolarite.class));
    }

    @Test
    void shouldRejectCreationWhenStudentDoesNotExist() {
        when(scolariteRepository
                .existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebut(
                        request.idEleve(),
                        request.idClasse(),
                        request.dateDebut()
                ))
                .thenReturn(false);

        when(eleveRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                scolariteService.create(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'élève ayant l'identifiant "
                                + "1 est introuvable."
                );

        verifyNoInteractions(
                classeRepository,
                scolariteMapper
        );

        verify(scolariteRepository, never())
                .save(any(Scolarite.class));
    }

    @Test
    void shouldRejectCreationWhenClassDoesNotExist() {
        when(scolariteRepository
                .existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebut(
                        request.idEleve(),
                        request.idClasse(),
                        request.dateDebut()
                ))
                .thenReturn(false);

        when(eleveRepository.findById(1L))
                .thenReturn(Optional.of(eleve));

        when(classeRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                scolariteService.create(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La classe ayant l'identifiant "
                                + "2 est introuvable."
                );

        verifyNoInteractions(scolariteMapper);

        verify(scolariteRepository, never())
                .save(any(Scolarite.class));
    }

    @Test
    void shouldUpdateScolarite() {
        when(scolariteRepository.findById(3L))
                .thenReturn(Optional.of(scolarite));

        when(scolariteRepository
                .existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebutAndIdScolariteNot(
                        request.idEleve(),
                        request.idClasse(),
                        request.dateDebut(),
                        3L
                ))
                .thenReturn(false);

        when(eleveRepository.findById(1L))
                .thenReturn(Optional.of(eleve));

        when(classeRepository.findById(2L))
                .thenReturn(Optional.of(classe));

        when(scolariteRepository.save(scolarite))
                .thenReturn(scolarite);

        when(scolariteMapper.toResponse(scolarite))
                .thenReturn(response);

        ScolariteResponse result =
                scolariteService.update(3L, request);

        assertThat(result).isEqualTo(response);

        verify(scolariteMapper).updateEntity(
                request,
                eleve,
                classe,
                scolarite
        );

        verify(scolariteRepository).save(scolarite);
        verify(scolariteMapper).toResponse(scolarite);
    }

    @Test
    void shouldRejectDuplicateScolariteOnUpdate() {
        when(scolariteRepository.findById(3L))
                .thenReturn(Optional.of(scolarite));

        when(scolariteRepository
                .existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebutAndIdScolariteNot(
                        request.idEleve(),
                        request.idClasse(),
                        request.dateDebut(),
                        3L
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                scolariteService.update(3L, request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("1")
                .hasMessageContaining("2")
                .hasMessageContaining("2026-09-01");

        verifyNoInteractions(
                eleveRepository,
                classeRepository,
                scolariteMapper
        );

        verify(scolariteRepository, never())
                .save(any(Scolarite.class));
    }

    @Test
    void shouldRejectUpdateWhenStudentDoesNotExist() {
        when(scolariteRepository.findById(3L))
                .thenReturn(Optional.of(scolarite));

        when(scolariteRepository
                .existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebutAndIdScolariteNot(
                        request.idEleve(),
                        request.idClasse(),
                        request.dateDebut(),
                        3L
                ))
                .thenReturn(false);

        when(eleveRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                scolariteService.update(3L, request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'élève ayant l'identifiant "
                                + "1 est introuvable."
                );

        verifyNoInteractions(
                classeRepository,
                scolariteMapper
        );

        verify(scolariteRepository, never())
                .save(any(Scolarite.class));
    }

    @Test
    void shouldRejectUpdateWhenClassDoesNotExist() {
        when(scolariteRepository.findById(3L))
                .thenReturn(Optional.of(scolarite));

        when(scolariteRepository
                .existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebutAndIdScolariteNot(
                        request.idEleve(),
                        request.idClasse(),
                        request.dateDebut(),
                        3L
                ))
                .thenReturn(false);

        when(eleveRepository.findById(1L))
                .thenReturn(Optional.of(eleve));

        when(classeRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                scolariteService.update(3L, request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La classe ayant l'identifiant "
                                + "2 est introuvable."
                );

        verifyNoInteractions(scolariteMapper);

        verify(scolariteRepository, never())
                .save(any(Scolarite.class));
    }

    @Test
    void shouldDeleteScolarite() {
        when(scolariteRepository.findById(3L))
                .thenReturn(Optional.of(scolarite));

        scolariteService.delete(3L);

        verify(scolariteRepository).findById(3L);
        verify(scolariteRepository).delete(scolarite);
    }

    @Test
    void shouldRejectDeletionWhenScolariteDoesNotExist() {
        when(scolariteRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                scolariteService.delete(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La scolarité ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(scolariteRepository).findById(99L);
        verify(scolariteRepository, never())
                .delete(any(Scolarite.class));
    }
}