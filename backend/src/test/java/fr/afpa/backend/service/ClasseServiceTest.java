package fr.afpa.backend.service;

import fr.afpa.backend.dto.classe.ClasseRequest;
import fr.afpa.backend.dto.classe.ClasseResponse;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Enseignant;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.ClasseMapper;
import fr.afpa.backend.repository.ClasseRepository;
import fr.afpa.backend.repository.EnseignantRepository;
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
class ClasseServiceTest {

    @Mock
    private ClasseRepository classeRepository;

    @Mock
    private EnseignantRepository enseignantRepository;

    @Mock
    private ClasseMapper classeMapper;

    private ClasseService classeService;

    private ClasseRequest request;
    private ClasseRequest requestWithoutProfessor;
    private Enseignant professeurPrincipal;
    private Classe classe;
    private ClasseResponse response;

    @BeforeEach
    void setUp() {
        classeService = new ClasseService(
                classeRepository,
                enseignantRepository,
                classeMapper
        );

        request = new ClasseRequest(
                "6A",
                "Sixième",
                "2026-2027",
                10L
        );

        requestWithoutProfessor = new ClasseRequest(
                "6A",
                "Sixième",
                "2026-2027",
                null
        );

        professeurPrincipal = new Enseignant(
                "Martin",
                "Paul",
                "paul.martin@example.com",
                "0611223344",
                "12 rue des Écoles",
                "ENS-001"
        );

        classe = new Classe(
                request.nom(),
                request.niveau(),
                request.anneeScolaire(),
                professeurPrincipal
        );

        response = new ClasseResponse(
                1L,
                request.nom(),
                request.niveau(),
                request.anneeScolaire(),
                request.idProfesseurPrincipal()
        );
    }

    @Test
    void shouldReturnAllClasses() {
        when(classeRepository.findAll())
                .thenReturn(List.of(classe));

        when(classeMapper.toResponse(classe))
                .thenReturn(response);

        List<ClasseResponse> result = classeService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(classeRepository).findAll();
        verify(classeMapper).toResponse(classe);
    }

    @Test
    void shouldReturnClassById() {
        when(classeRepository.findById(1L))
                .thenReturn(Optional.of(classe));

        when(classeMapper.toResponse(classe))
                .thenReturn(response);

        ClasseResponse result = classeService.findById(1L);

        assertThat(result).isEqualTo(response);

        verify(classeRepository).findById(1L);
        verify(classeMapper).toResponse(classe);
    }

    @Test
    void shouldThrowExceptionWhenClassDoesNotExist() {
        when(classeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> classeService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La classe ayant l'identifiant 99 est introuvable."
                );

        verify(classeRepository).findById(99L);
        verifyNoInteractions(classeMapper);
    }

    @Test
    void shouldCreateClassWithMainTeacher() {
        when(classeRepository.existsByNomAndAnneeScolaire(
                request.nom(),
                request.anneeScolaire()
        )).thenReturn(false);

        when(enseignantRepository.findById(
                request.idProfesseurPrincipal()
        )).thenReturn(Optional.of(professeurPrincipal));

        when(classeMapper.toEntity(
                request,
                professeurPrincipal
        )).thenReturn(classe);

        when(classeRepository.save(classe))
                .thenReturn(classe);

        when(classeMapper.toResponse(classe))
                .thenReturn(response);

        ClasseResponse result = classeService.create(request);

        assertThat(result).isEqualTo(response);

        verify(classeRepository)
                .existsByNomAndAnneeScolaire(
                        request.nom(),
                        request.anneeScolaire()
                );

        verify(enseignantRepository)
                .findById(request.idProfesseurPrincipal());

        verify(classeMapper).toEntity(
                request,
                professeurPrincipal
        );

        verify(classeRepository).save(classe);
        verify(classeMapper).toResponse(classe);
    }

    @Test
    void shouldCreateClassWithoutMainTeacher() {
        Classe classeWithoutProfessor = new Classe(
                requestWithoutProfessor.nom(),
                requestWithoutProfessor.niveau(),
                requestWithoutProfessor.anneeScolaire(),
                null
        );

        ClasseResponse responseWithoutProfessor =
                new ClasseResponse(
                        1L,
                        requestWithoutProfessor.nom(),
                        requestWithoutProfessor.niveau(),
                        requestWithoutProfessor.anneeScolaire(),
                        null
                );

        when(classeRepository.existsByNomAndAnneeScolaire(
                requestWithoutProfessor.nom(),
                requestWithoutProfessor.anneeScolaire()
        )).thenReturn(false);

        when(classeMapper.toEntity(
                requestWithoutProfessor,
                null
        )).thenReturn(classeWithoutProfessor);

        when(classeRepository.save(classeWithoutProfessor))
                .thenReturn(classeWithoutProfessor);

        when(classeMapper.toResponse(classeWithoutProfessor))
                .thenReturn(responseWithoutProfessor);

        ClasseResponse result =
                classeService.create(requestWithoutProfessor);

        assertThat(result).isEqualTo(responseWithoutProfessor);

        verifyNoInteractions(enseignantRepository);

        verify(classeMapper).toEntity(
                requestWithoutProfessor,
                null
        );

        verify(classeRepository).save(classeWithoutProfessor);
    }

    @Test
    void shouldRejectDuplicateClassOnCreation() {
        when(classeRepository.existsByNomAndAnneeScolaire(
                request.nom(),
                request.anneeScolaire()
        )).thenReturn(true);

        assertThatThrownBy(() -> classeService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(request.nom())
                .hasMessageContaining(request.anneeScolaire());

        verify(classeRepository)
                .existsByNomAndAnneeScolaire(
                        request.nom(),
                        request.anneeScolaire()
                );

        verifyNoInteractions(enseignantRepository);
        verifyNoInteractions(classeMapper);
        verify(classeRepository, never()).save(classe);
    }

    @Test
    void shouldRejectCreationWhenMainTeacherDoesNotExist() {
        when(classeRepository.existsByNomAndAnneeScolaire(
                request.nom(),
                request.anneeScolaire()
        )).thenReturn(false);

        when(enseignantRepository.findById(
                request.idProfesseurPrincipal()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classeService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'enseignant ayant l'identifiant 10 est introuvable."
                );

        verify(enseignantRepository).findById(10L);
        verifyNoInteractions(classeMapper);
        verify(classeRepository, never()).save(classe);
    }

    @Test
    void shouldUpdateClass() {
        when(classeRepository.findById(1L))
                .thenReturn(Optional.of(classe));

        when(classeRepository
                .existsByNomAndAnneeScolaireAndIdClasseNot(
                        requestWithoutProfessor.nom(),
                        requestWithoutProfessor.anneeScolaire(),
                        1L
                ))
                .thenReturn(false);

        when(classeRepository.save(classe))
                .thenReturn(classe);

        ClasseResponse responseWithoutProfessor =
                new ClasseResponse(
                        1L,
                        requestWithoutProfessor.nom(),
                        requestWithoutProfessor.niveau(),
                        requestWithoutProfessor.anneeScolaire(),
                        null
                );

        when(classeMapper.toResponse(classe))
                .thenReturn(responseWithoutProfessor);

        ClasseResponse result = classeService.update(
                1L,
                requestWithoutProfessor
        );

        assertThat(result).isEqualTo(responseWithoutProfessor);

        verify(classeRepository).findById(1L);

        verify(classeRepository)
                .existsByNomAndAnneeScolaireAndIdClasseNot(
                        requestWithoutProfessor.nom(),
                        requestWithoutProfessor.anneeScolaire(),
                        1L
                );

        verifyNoInteractions(enseignantRepository);

        verify(classeMapper).updateEntity(
                requestWithoutProfessor,
                null,
                classe
        );

        verify(classeRepository).save(classe);
        verify(classeMapper).toResponse(classe);
    }

    @Test
    void shouldRejectDuplicateClassOnUpdate() {
        when(classeRepository.findById(1L))
                .thenReturn(Optional.of(classe));

        when(classeRepository
                .existsByNomAndAnneeScolaireAndIdClasseNot(
                        request.nom(),
                        request.anneeScolaire(),
                        1L
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                classeService.update(1L, request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(request.nom())
                .hasMessageContaining(request.anneeScolaire());

        verifyNoInteractions(enseignantRepository);

        verify(classeMapper, never()).updateEntity(
                request,
                professeurPrincipal,
                classe
        );

        verify(classeRepository, never()).save(classe);
    }

    @Test
    void shouldDeleteClass() {
        when(classeRepository.findById(1L))
                .thenReturn(Optional.of(classe));

        classeService.delete(1L);

        verify(classeRepository).findById(1L);
        verify(classeRepository).delete(classe);
    }

    @Test
    void shouldRejectDeletionWhenClassDoesNotExist() {
        when(classeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> classeService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La classe ayant l'identifiant 99 est introuvable."
                );

        verify(classeRepository).findById(99L);
        verify(classeRepository, never()).delete(classe);
    }
}