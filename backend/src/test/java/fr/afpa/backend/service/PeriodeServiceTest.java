package fr.afpa.backend.service;

import fr.afpa.backend.dto.periode.PeriodeRequest;
import fr.afpa.backend.dto.periode.PeriodeResponse;
import fr.afpa.backend.entity.Periode;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.PeriodeMapper;
import fr.afpa.backend.repository.PeriodeRepository;
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
class PeriodeServiceTest {

    @Mock
    private PeriodeRepository periodeRepository;

    @Mock
    private PeriodeMapper periodeMapper;

    private PeriodeService periodeService;

    private PeriodeRequest request;
    private Periode periode;
    private PeriodeResponse response;

    @BeforeEach
    void setUp() {
        periodeService = new PeriodeService(
                periodeRepository,
                periodeMapper
        );

        request = new PeriodeRequest(
                "Trimestre 1",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 18),
                LocalDate.of(2026, 11, 30),
                LocalDate.of(2026, 12, 15),
                "OUVERTE"
        );

        periode = new Periode(
                request.libelle(),
                request.dateDebut(),
                request.dateFin(),
                request.dateDebutSaisie(),
                request.dateFinSaisie(),
                request.statut()
        );

        response = new PeriodeResponse(
                1L,
                request.libelle(),
                request.dateDebut(),
                request.dateFin(),
                request.dateDebutSaisie(),
                request.dateFinSaisie(),
                request.statut()
        );
    }

    @Test
    void shouldReturnAllPeriods() {
        when(periodeRepository.findAll())
                .thenReturn(List.of(periode));

        when(periodeMapper.toResponse(periode))
                .thenReturn(response);

        List<PeriodeResponse> result =
                periodeService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(periodeRepository).findAll();
        verify(periodeMapper).toResponse(periode);
    }

    @Test
    void shouldReturnPeriodById() {
        when(periodeRepository.findById(1L))
                .thenReturn(Optional.of(periode));

        when(periodeMapper.toResponse(periode))
                .thenReturn(response);

        PeriodeResponse result =
                periodeService.findById(1L);

        assertThat(result).isEqualTo(response);

        verify(periodeRepository).findById(1L);
        verify(periodeMapper).toResponse(periode);
    }

    @Test
    void shouldThrowExceptionWhenPeriodDoesNotExist() {
        when(periodeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                periodeService.findById(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La période ayant l'identifiant 99 est introuvable."
                );

        verify(periodeRepository).findById(99L);
        verifyNoInteractions(periodeMapper);
    }

    @Test
    void shouldCreatePeriod() {
        when(periodeMapper.toEntity(request))
                .thenReturn(periode);

        when(periodeRepository.save(periode))
                .thenReturn(periode);

        when(periodeMapper.toResponse(periode))
                .thenReturn(response);

        PeriodeResponse result =
                periodeService.create(request);

        assertThat(result).isEqualTo(response);

        verify(periodeMapper).toEntity(request);
        verify(periodeRepository).save(periode);
        verify(periodeMapper).toResponse(periode);
    }

    @Test
    void shouldUpdatePeriod() {
        when(periodeRepository.findById(1L))
                .thenReturn(Optional.of(periode));

        when(periodeRepository.save(periode))
                .thenReturn(periode);

        when(periodeMapper.toResponse(periode))
                .thenReturn(response);

        PeriodeResponse result =
                periodeService.update(1L, request);

        assertThat(result).isEqualTo(response);

        verify(periodeRepository).findById(1L);

        verify(periodeMapper).updateEntity(
                request,
                periode
        );

        verify(periodeRepository).save(periode);
        verify(periodeMapper).toResponse(periode);
    }

    @Test
    void shouldRejectUpdateWhenPeriodDoesNotExist() {
        when(periodeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                periodeService.update(99L, request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La période ayant l'identifiant 99 est introuvable."
                );

        verify(periodeRepository).findById(99L);

        verify(periodeMapper, never()).updateEntity(
                request,
                periode
        );

        verify(periodeRepository, never()).save(periode);
    }

    @Test
    void shouldDeletePeriod() {
        when(periodeRepository.findById(1L))
                .thenReturn(Optional.of(periode));

        periodeService.delete(1L);

        verify(periodeRepository).findById(1L);
        verify(periodeRepository).delete(periode);
    }

    @Test
    void shouldRejectDeletionWhenPeriodDoesNotExist() {
        when(periodeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                periodeService.delete(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La période ayant l'identifiant 99 est introuvable."
                );

        verify(periodeRepository).findById(99L);
        verify(periodeRepository, never()).delete(periode);
    }
}