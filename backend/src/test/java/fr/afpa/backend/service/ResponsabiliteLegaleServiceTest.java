package fr.afpa.backend.service;

import fr.afpa.backend.dto.responsabilitelegale.ResponsabiliteLegaleRequest;
import fr.afpa.backend.dto.responsabilitelegale.ResponsabiliteLegaleResponse;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.ResponsabiliteLegale;
import fr.afpa.backend.entity.ResponsabiliteLegaleId;
import fr.afpa.backend.entity.Responsable;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.ResponsabiliteLegaleMapper;
import fr.afpa.backend.repository.EleveRepository;
import fr.afpa.backend.repository.ResponsabiliteLegaleRepository;
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
class ResponsabiliteLegaleServiceTest {

    @Mock
    private ResponsabiliteLegaleRepository
            responsabiliteLegaleRepository;

    @Mock
    private ResponsableRepository responsableRepository;

    @Mock
    private EleveRepository eleveRepository;

    @Mock
    private ResponsabiliteLegaleMapper
            responsabiliteLegaleMapper;

    @Mock
    private Responsable responsable;

    @Mock
    private Eleve eleve;

    @Mock
    private ResponsabiliteLegale responsabiliteLegale;

    private ResponsabiliteLegaleService
            responsabiliteLegaleService;

    private ResponsabiliteLegaleRequest request;
    private ResponsabiliteLegaleResponse response;
    private ResponsabiliteLegaleId id;

    @BeforeEach
    void setUp() {
        responsabiliteLegaleService =
                new ResponsabiliteLegaleService(
                        responsabiliteLegaleRepository,
                        responsableRepository,
                        eleveRepository,
                        responsabiliteLegaleMapper
                );

        request = new ResponsabiliteLegaleRequest(
                1L,
                2L
        );

        response = new ResponsabiliteLegaleResponse(
                1L,
                "Dupont",
                "Claire",
                2L,
                "Martin",
                "Lucas"
        );

        id = new ResponsabiliteLegaleId(
                request.idResponsable(),
                request.idEleve()
        );
    }

    @Test
    void shouldReturnAllLegalResponsibilities() {
        when(responsabiliteLegaleRepository.findAll())
                .thenReturn(List.of(responsabiliteLegale));

        when(responsabiliteLegaleMapper.toResponse(
                responsabiliteLegale
        )).thenReturn(response);

        List<ResponsabiliteLegaleResponse> result =
                responsabiliteLegaleService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(responsabiliteLegaleRepository).findAll();

        verify(responsabiliteLegaleMapper).toResponse(
                responsabiliteLegale
        );
    }

    @Test
    void shouldReturnLegalResponsibilityById() {
        when(responsabiliteLegaleRepository.findById(id))
                .thenReturn(Optional.of(responsabiliteLegale));

        when(responsabiliteLegaleMapper.toResponse(
                responsabiliteLegale
        )).thenReturn(response);

        ResponsabiliteLegaleResponse result =
                responsabiliteLegaleService.findById(
                        1L,
                        2L
                );

        assertThat(result).isEqualTo(response);

        verify(responsabiliteLegaleRepository)
                .findById(id);

        verify(responsabiliteLegaleMapper)
                .toResponse(responsabiliteLegale);
    }

    @Test
    void shouldThrowExceptionWhenLegalResponsibilityDoesNotExist() {
        when(responsabiliteLegaleRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                responsabiliteLegaleService.findById(
                        1L,
                        2L
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La responsabilité légale entre le responsable "
                                + "1 et l'élève 2 est introuvable."
                );

        verify(responsabiliteLegaleRepository)
                .findById(id);

        verifyNoInteractions(responsabiliteLegaleMapper);
    }

    @Test
    void shouldCreateLegalResponsibility() {
        when(responsabiliteLegaleRepository.existsById(id))
                .thenReturn(false);

        when(responsableRepository.findById(1L))
                .thenReturn(Optional.of(responsable));

        when(eleveRepository.findById(2L))
                .thenReturn(Optional.of(eleve));

        when(responsabiliteLegaleMapper.toEntity(
                responsable,
                eleve
        )).thenReturn(responsabiliteLegale);

        when(responsabiliteLegaleRepository.save(
                responsabiliteLegale
        )).thenReturn(responsabiliteLegale);

        when(responsabiliteLegaleMapper.toResponse(
                responsabiliteLegale
        )).thenReturn(response);

        ResponsabiliteLegaleResponse result =
                responsabiliteLegaleService.create(request);

        assertThat(result).isEqualTo(response);

        verify(responsabiliteLegaleRepository)
                .existsById(id);

        verify(responsableRepository).findById(1L);
        verify(eleveRepository).findById(2L);

        verify(responsabiliteLegaleMapper).toEntity(
                responsable,
                eleve
        );

        verify(responsabiliteLegaleRepository).save(
                responsabiliteLegale
        );

        verify(responsabiliteLegaleMapper).toResponse(
                responsabiliteLegale
        );
    }

    @Test
    void shouldRejectDuplicateLegalResponsibility() {
        when(responsabiliteLegaleRepository.existsById(id))
                .thenReturn(true);

        assertThatThrownBy(() ->
                responsabiliteLegaleService.create(request)
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "La responsabilité légale entre le responsable "
                                + "1 et l'élève 2 existe déjà."
                );

        verify(responsabiliteLegaleRepository)
                .existsById(id);

        verifyNoInteractions(
                responsableRepository,
                eleveRepository,
                responsabiliteLegaleMapper
        );

        verify(responsabiliteLegaleRepository, never())
                .save(responsabiliteLegale);
    }

    @Test
    void shouldRejectCreationWhenGuardianDoesNotExist() {
        when(responsabiliteLegaleRepository.existsById(id))
                .thenReturn(false);

        when(responsableRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                responsabiliteLegaleService.create(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Le responsable ayant l'identifiant "
                                + "1 est introuvable."
                );

        verify(responsabiliteLegaleRepository)
                .existsById(id);

        verify(responsableRepository).findById(1L);

        verifyNoInteractions(
                eleveRepository,
                responsabiliteLegaleMapper
        );

        verify(responsabiliteLegaleRepository, never())
                .save(responsabiliteLegale);
    }

    @Test
    void shouldRejectCreationWhenStudentDoesNotExist() {
        when(responsabiliteLegaleRepository.existsById(id))
                .thenReturn(false);

        when(responsableRepository.findById(1L))
                .thenReturn(Optional.of(responsable));

        when(eleveRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                responsabiliteLegaleService.create(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'élève ayant l'identifiant "
                                + "2 est introuvable."
                );

        verify(responsabiliteLegaleRepository)
                .existsById(id);

        verify(responsableRepository).findById(1L);
        verify(eleveRepository).findById(2L);

        verifyNoInteractions(responsabiliteLegaleMapper);

        verify(responsabiliteLegaleRepository, never())
                .save(responsabiliteLegale);
    }

    @Test
    void shouldDeleteLegalResponsibility() {
        when(responsabiliteLegaleRepository.findById(id))
                .thenReturn(Optional.of(responsabiliteLegale));

        responsabiliteLegaleService.delete(
                1L,
                2L
        );

        verify(responsabiliteLegaleRepository)
                .findById(id);

        verify(responsabiliteLegaleRepository)
                .delete(responsabiliteLegale);
    }

    @Test
    void shouldRejectDeletionWhenLegalResponsibilityDoesNotExist() {
        when(responsabiliteLegaleRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                responsabiliteLegaleService.delete(
                        1L,
                        2L
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La responsabilité légale entre le responsable "
                                + "1 et l'élève 2 est introuvable."
                );

        verify(responsabiliteLegaleRepository)
                .findById(id);

        verify(responsabiliteLegaleRepository, never())
                .delete(responsabiliteLegale);
    }
}
