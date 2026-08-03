package fr.afpa.backend.service;

import fr.afpa.backend.dto.responsable.ResponsableRequest;
import fr.afpa.backend.dto.responsable.ResponsableResponse;
import fr.afpa.backend.entity.Responsable;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.ResponsableMapper;
import fr.afpa.backend.repository.ResponsableRepository;
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
class ResponsableServiceTest {

    @Mock
    private ResponsableRepository responsableRepository;

    @Mock
    private ResponsableMapper responsableMapper;

    private ResponsableService responsableService;

    private ResponsableRequest request;
    private Responsable responsable;
    private ResponsableResponse response;

    @BeforeEach
    void setUp() {
        responsableService = new ResponsableService(
                responsableRepository,
                responsableMapper
        );

        request = new ResponsableRequest(
                "Dupont",
                "Claire",
                "claire.dupont@example.com",
                "0612345678",
                "12 rue des Écoles",
                "Médecin"
        );

        responsable = new Responsable(
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.profession()
        );

        response = new ResponsableResponse(
                1L,
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.profession()
        );
    }

    @Test
    void shouldReturnAllGuardians() {
        when(responsableRepository.findAll())
                .thenReturn(List.of(responsable));

        when(responsableMapper.toResponse(responsable))
                .thenReturn(response);

        List<ResponsableResponse> result =
                responsableService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(responsableRepository).findAll();
        verify(responsableMapper).toResponse(responsable);
    }

    @Test
    void shouldReturnGuardianById() {
        when(responsableRepository.findById(1L))
                .thenReturn(Optional.of(responsable));

        when(responsableMapper.toResponse(responsable))
                .thenReturn(response);

        ResponsableResponse result =
                responsableService.findById(1L);

        assertThat(result).isEqualTo(response);

        verify(responsableRepository).findById(1L);
        verify(responsableMapper).toResponse(responsable);
    }

    @Test
    void shouldThrowExceptionWhenGuardianDoesNotExist() {
        when(responsableRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                responsableService.findById(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Le responsable ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(responsableRepository).findById(99L);
        verifyNoInteractions(responsableMapper);
    }

    @Test
    void shouldCreateGuardian() {
        when(responsableMapper.toEntity(request))
                .thenReturn(responsable);

        when(responsableRepository.save(responsable))
                .thenReturn(responsable);

        when(responsableMapper.toResponse(responsable))
                .thenReturn(response);

        ResponsableResponse result =
                responsableService.create(request);

        assertThat(result).isEqualTo(response);

        verify(responsableMapper).toEntity(request);
        verify(responsableRepository).save(responsable);
        verify(responsableMapper).toResponse(responsable);
    }

    @Test
    void shouldUpdateGuardian() {
        when(responsableRepository.findById(1L))
                .thenReturn(Optional.of(responsable));

        when(responsableRepository.save(responsable))
                .thenReturn(responsable);

        when(responsableMapper.toResponse(responsable))
                .thenReturn(response);

        ResponsableResponse result =
                responsableService.update(1L, request);

        assertThat(result).isEqualTo(response);

        verify(responsableRepository).findById(1L);

        verify(responsableMapper).updateEntity(
                request,
                responsable
        );

        verify(responsableRepository).save(responsable);
        verify(responsableMapper).toResponse(responsable);
    }

    @Test
    void shouldDeleteGuardian() {
        when(responsableRepository.findById(1L))
                .thenReturn(Optional.of(responsable));

        responsableService.delete(1L);

        verify(responsableRepository).findById(1L);
        verify(responsableRepository).delete(responsable);
    }

    @Test
    void shouldRejectDeletionWhenGuardianDoesNotExist() {
        when(responsableRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                responsableService.delete(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Le responsable ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(responsableRepository).findById(99L);

        verify(responsableRepository, never())
                .delete(responsable);

        verifyNoInteractions(responsableMapper);
    }
}
