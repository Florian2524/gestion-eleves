package fr.afpa.backend.service;

import fr.afpa.backend.dto.matiere.MatiereRequest;
import fr.afpa.backend.dto.matiere.MatiereResponse;
import fr.afpa.backend.entity.Matiere;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.MatiereMapper;
import fr.afpa.backend.repository.MatiereRepository;
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
class MatiereServiceTest {

    @Mock
    private MatiereRepository matiereRepository;

    @Mock
    private MatiereMapper matiereMapper;

    private MatiereService matiereService;

    private MatiereRequest request;
    private Matiere matiere;
    private MatiereResponse response;

    @BeforeEach
    void setUp() {
        matiereService = new MatiereService(
                matiereRepository,
                matiereMapper
        );

        request = new MatiereRequest(
                "MATH",
                "Mathématiques"
        );

        matiere = new Matiere(
                request.code(),
                request.nom()
        );

        response = new MatiereResponse(
                1L,
                request.code(),
                request.nom()
        );
    }

    @Test
    void shouldReturnAllSubjects() {
        when(matiereRepository.findAll())
                .thenReturn(List.of(matiere));

        when(matiereMapper.toResponse(matiere))
                .thenReturn(response);

        List<MatiereResponse> result =
                matiereService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(matiereRepository).findAll();
        verify(matiereMapper).toResponse(matiere);
    }

    @Test
    void shouldReturnSubjectById() {
        when(matiereRepository.findById(1L))
                .thenReturn(Optional.of(matiere));

        when(matiereMapper.toResponse(matiere))
                .thenReturn(response);

        MatiereResponse result =
                matiereService.findById(1L);

        assertThat(result).isEqualTo(response);

        verify(matiereRepository).findById(1L);
        verify(matiereMapper).toResponse(matiere);
    }

    @Test
    void shouldThrowExceptionWhenSubjectDoesNotExist() {
        when(matiereRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                matiereService.findById(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La matière ayant l'identifiant 99 est introuvable."
                );

        verify(matiereRepository).findById(99L);
        verifyNoInteractions(matiereMapper);
    }

    @Test
    void shouldCreateSubject() {
        when(matiereRepository.existsByCode(request.code()))
                .thenReturn(false);

        when(matiereMapper.toEntity(request))
                .thenReturn(matiere);

        when(matiereRepository.save(matiere))
                .thenReturn(matiere);

        when(matiereMapper.toResponse(matiere))
                .thenReturn(response);

        MatiereResponse result =
                matiereService.create(request);

        assertThat(result).isEqualTo(response);

        verify(matiereRepository)
                .existsByCode(request.code());

        verify(matiereMapper).toEntity(request);
        verify(matiereRepository).save(matiere);
        verify(matiereMapper).toResponse(matiere);
    }

    @Test
    void shouldRejectDuplicateCodeOnCreation() {
        when(matiereRepository.existsByCode(request.code()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                matiereService.create(request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "Une matière possède déjà le code "
                                + request.code()
                                + "."
                );

        verify(matiereRepository)
                .existsByCode(request.code());

        verifyNoInteractions(matiereMapper);
        verify(matiereRepository, never()).save(matiere);
    }

    @Test
    void shouldUpdateSubject() {
        when(matiereRepository.findById(1L))
                .thenReturn(Optional.of(matiere));

        when(matiereRepository
                .existsByCodeAndIdMatiereNot(
                        request.code(),
                        1L
                ))
                .thenReturn(false);

        when(matiereRepository.save(matiere))
                .thenReturn(matiere);

        when(matiereMapper.toResponse(matiere))
                .thenReturn(response);

        MatiereResponse result = matiereService.update(
                1L,
                request
        );

        assertThat(result).isEqualTo(response);

        verify(matiereRepository).findById(1L);

        verify(matiereRepository)
                .existsByCodeAndIdMatiereNot(
                        request.code(),
                        1L
                );

        verify(matiereMapper)
                .updateEntity(request, matiere);

        verify(matiereRepository).save(matiere);
        verify(matiereMapper).toResponse(matiere);
    }

    @Test
    void shouldRejectDuplicateCodeOnUpdate() {
        when(matiereRepository.findById(1L))
                .thenReturn(Optional.of(matiere));

        when(matiereRepository
                .existsByCodeAndIdMatiereNot(
                        request.code(),
                        1L
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                matiereService.update(1L, request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "Une autre matière possède déjà le code "
                                + request.code()
                                + "."
                );

        verify(matiereRepository).findById(1L);

        verify(matiereRepository)
                .existsByCodeAndIdMatiereNot(
                        request.code(),
                        1L
                );

        verify(matiereMapper, never())
                .updateEntity(request, matiere);

        verify(matiereRepository, never()).save(matiere);
    }

    @Test
    void shouldDeleteSubject() {
        when(matiereRepository.findById(1L))
                .thenReturn(Optional.of(matiere));

        matiereService.delete(1L);

        verify(matiereRepository).findById(1L);
        verify(matiereRepository).delete(matiere);
    }

    @Test
    void shouldRejectDeletionWhenSubjectDoesNotExist() {
        when(matiereRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                matiereService.delete(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La matière ayant l'identifiant 99 est introuvable."
                );

        verify(matiereRepository).findById(99L);
        verify(matiereRepository, never()).delete(matiere);
    }
}