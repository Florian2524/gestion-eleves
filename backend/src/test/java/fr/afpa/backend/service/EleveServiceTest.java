package fr.afpa.backend.service;

import fr.afpa.backend.dto.eleve.EleveRequest;
import fr.afpa.backend.dto.eleve.EleveResponse;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.EleveMapper;
import fr.afpa.backend.repository.EleveRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EleveServiceTest {

    @Mock
    private EleveRepository eleveRepository;

    @Mock
    private EleveMapper eleveMapper;

    private EleveService eleveService;

    private EleveRequest request;
    private Eleve eleve;
    private EleveResponse response;

    @BeforeEach
    void setUp() {
        eleveService = new EleveService(
                eleveRepository,
                eleveMapper
        );

        request = new EleveRequest(
                "Dupont",
                "Alice",
                "alice.dupont@example.com",
                "0612345678",
                "10 rue de Paris",
                "ELEVE-001",
                LocalDate.of(2012, 5, 15),
                null
        );

        eleve = new Eleve(
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.matricule(),
                request.dateNaissance(),
                request.photoUrl()
        );

        response = new EleveResponse(
                1L,
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.matricule(),
                request.dateNaissance(),
                request.photoUrl()
        );
    }

    @Test
    void shouldReturnAllStudents() {
        when(eleveRepository.findAll()).thenReturn(List.of(eleve));
        when(eleveMapper.toResponse(eleve)).thenReturn(response);

        List<EleveResponse> result = eleveService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(eleveRepository).findAll();
        verify(eleveMapper).toResponse(eleve);
    }

    @Test
    void shouldReturnStudentById() {
        when(eleveRepository.findById(1L))
                .thenReturn(Optional.of(eleve));

        when(eleveMapper.toResponse(eleve))
                .thenReturn(response);

        EleveResponse result = eleveService.findById(1L);

        assertThat(result).isEqualTo(response);

        verify(eleveRepository).findById(1L);
        verify(eleveMapper).toResponse(eleve);
    }

    @Test
    void shouldThrowExceptionWhenStudentDoesNotExist() {
        when(eleveRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eleveService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'élève ayant l'identifiant 99 est introuvable."
                );

        verify(eleveRepository).findById(99L);
        verifyNoInteractions(eleveMapper);
    }

    @Test
    void shouldCreateStudent() {
        when(eleveRepository.existsByMatricule(
                request.matricule()
        )).thenReturn(false);

        when(eleveMapper.toEntity(request))
                .thenReturn(eleve);

        when(eleveRepository.save(eleve))
                .thenReturn(eleve);

        when(eleveMapper.toResponse(eleve))
                .thenReturn(response);

        EleveResponse result = eleveService.create(request);

        assertThat(result).isEqualTo(response);

        verify(eleveRepository)
                .existsByMatricule(request.matricule());

        verify(eleveMapper).toEntity(request);
        verify(eleveRepository).save(eleve);
        verify(eleveMapper).toResponse(eleve);
    }

    @Test
    void shouldRejectDuplicateMatriculeOnCreation() {
        when(eleveRepository.existsByMatricule(
                request.matricule()
        )).thenReturn(true);

        assertThatThrownBy(() -> eleveService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "Un élève possède déjà le matricule "
                                + request.matricule()
                                + "."
                );

        verify(eleveRepository)
                .existsByMatricule(request.matricule());

        verify(eleveRepository, never()).save(eleve);
        verifyNoInteractions(eleveMapper);
    }

    @Test
    void shouldUpdateStudent() {
        when(eleveRepository.findById(1L))
                .thenReturn(Optional.of(eleve));

        when(eleveRepository
                .existsByMatriculeAndIdPersonneNot(
                        request.matricule(),
                        1L
                ))
                .thenReturn(false);

        when(eleveRepository.save(eleve))
                .thenReturn(eleve);

        when(eleveMapper.toResponse(eleve))
                .thenReturn(response);

        EleveResponse result = eleveService.update(
                1L,
                request
        );

        assertThat(result).isEqualTo(response);

        verify(eleveRepository).findById(1L);

        verify(eleveRepository)
                .existsByMatriculeAndIdPersonneNot(
                        request.matricule(),
                        1L
                );

        verify(eleveMapper).updateEntity(request, eleve);
        verify(eleveRepository).save(eleve);
        verify(eleveMapper).toResponse(eleve);
    }

    @Test
    void shouldRejectDuplicateMatriculeOnUpdate() {
        when(eleveRepository.findById(1L))
                .thenReturn(Optional.of(eleve));

        when(eleveRepository
                .existsByMatriculeAndIdPersonneNot(
                        request.matricule(),
                        1L
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                eleveService.update(1L, request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "Un autre élève possède déjà le matricule "
                                + request.matricule()
                                + "."
                );

        verify(eleveRepository).findById(1L);

        verify(eleveRepository)
                .existsByMatriculeAndIdPersonneNot(
                        request.matricule(),
                        1L
                );

        verify(eleveMapper, never())
                .updateEntity(request, eleve);

        verify(eleveRepository, never()).save(eleve);
    }

    @Test
    void shouldDeleteStudent() {
        when(eleveRepository.findById(1L))
                .thenReturn(Optional.of(eleve));

        eleveService.delete(1L);

        verify(eleveRepository).findById(1L);
        verify(eleveRepository).delete(eleve);
    }

    @Test
    void shouldRejectDeletionWhenStudentDoesNotExist() {
        when(eleveRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eleveService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'élève ayant l'identifiant 99 est introuvable."
                );

        verify(eleveRepository).findById(99L);
        verify(eleveRepository, never()).delete(eleve);
    }
}