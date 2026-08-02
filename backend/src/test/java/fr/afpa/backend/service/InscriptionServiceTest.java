package fr.afpa.backend.service;

import fr.afpa.backend.dto.inscription.InscriptionRequest;
import fr.afpa.backend.dto.inscription.InscriptionResponse;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.Inscription;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.InscriptionMapper;
import fr.afpa.backend.repository.EleveRepository;
import fr.afpa.backend.repository.InscriptionRepository;
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
class InscriptionServiceTest {

    @Mock
    private InscriptionRepository inscriptionRepository;

    @Mock
    private EleveRepository eleveRepository;

    @Mock
    private InscriptionMapper inscriptionMapper;

    private InscriptionService inscriptionService;

    private InscriptionRequest request;
    private Eleve eleve;
    private Inscription inscription;
    private InscriptionResponse response;

    @BeforeEach
    void setUp() {
        inscriptionService = new InscriptionService(
                inscriptionRepository,
                eleveRepository,
                inscriptionMapper
        );

        request = new InscriptionRequest(
                2L,
                LocalDate.of(2026, 8, 15),
                null,
                "ACTIVE"
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

        inscription = new Inscription(
                eleve,
                request.dateInscription(),
                request.dateFin(),
                request.statut()
        );

        response = new InscriptionResponse(
                1L,
                request.idEleve(),
                request.dateInscription(),
                request.dateFin(),
                request.statut()
        );
    }

    @Test
    void shouldReturnAllInscriptions() {
        when(inscriptionRepository.findAll())
                .thenReturn(List.of(inscription));

        when(inscriptionMapper.toResponse(inscription))
                .thenReturn(response);

        List<InscriptionResponse> result =
                inscriptionService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(inscriptionRepository).findAll();
        verify(inscriptionMapper).toResponse(inscription);
    }

    @Test
    void shouldReturnInscriptionById() {
        when(inscriptionRepository.findById(1L))
                .thenReturn(Optional.of(inscription));

        when(inscriptionMapper.toResponse(inscription))
                .thenReturn(response);

        InscriptionResponse result =
                inscriptionService.findById(1L);

        assertThat(result).isEqualTo(response);

        verify(inscriptionRepository).findById(1L);
        verify(inscriptionMapper).toResponse(inscription);
    }

    @Test
    void shouldThrowExceptionWhenInscriptionDoesNotExist() {
        when(inscriptionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inscriptionService.findById(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'inscription ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(inscriptionRepository).findById(99L);
        verifyNoInteractions(inscriptionMapper);
    }

    @Test
    void shouldCreateInscription() {
        when(eleveRepository.findById(request.idEleve()))
                .thenReturn(Optional.of(eleve));

        when(inscriptionMapper.toEntity(
                request,
                eleve
        )).thenReturn(inscription);

        when(inscriptionRepository.save(inscription))
                .thenReturn(inscription);

        when(inscriptionMapper.toResponse(inscription))
                .thenReturn(response);

        InscriptionResponse result =
                inscriptionService.create(request);

        assertThat(result).isEqualTo(response);

        verify(eleveRepository).findById(2L);

        verify(inscriptionMapper).toEntity(
                request,
                eleve
        );

        verify(inscriptionRepository).save(inscription);
        verify(inscriptionMapper).toResponse(inscription);
    }

    @Test
    void shouldRejectCreationWhenStudentDoesNotExist() {
        when(eleveRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inscriptionService.create(request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'élève ayant l'identifiant "
                                + "2 est introuvable."
                );

        verify(eleveRepository).findById(2L);
        verifyNoInteractions(inscriptionMapper);

        verify(inscriptionRepository, never())
                .save(any(Inscription.class));
    }

    @Test
    void shouldUpdateInscription() {
        when(inscriptionRepository.findById(1L))
                .thenReturn(Optional.of(inscription));

        when(eleveRepository.findById(2L))
                .thenReturn(Optional.of(eleve));

        when(inscriptionRepository.save(inscription))
                .thenReturn(inscription);

        when(inscriptionMapper.toResponse(inscription))
                .thenReturn(response);

        InscriptionResponse result =
                inscriptionService.update(1L, request);

        assertThat(result).isEqualTo(response);

        verify(inscriptionRepository).findById(1L);
        verify(eleveRepository).findById(2L);

        verify(inscriptionMapper).updateEntity(
                request,
                eleve,
                inscription
        );

        verify(inscriptionRepository).save(inscription);
        verify(inscriptionMapper).toResponse(inscription);
    }

    @Test
    void shouldRejectUpdateWhenInscriptionDoesNotExist() {
        when(inscriptionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inscriptionService.update(99L, request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'inscription ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(inscriptionRepository).findById(99L);

        verifyNoInteractions(
                eleveRepository,
                inscriptionMapper
        );

        verify(inscriptionRepository, never())
                .save(any(Inscription.class));
    }

    @Test
    void shouldRejectUpdateWhenStudentDoesNotExist() {
        when(inscriptionRepository.findById(1L))
                .thenReturn(Optional.of(inscription));

        when(eleveRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inscriptionService.update(1L, request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'élève ayant l'identifiant "
                                + "2 est introuvable."
                );

        verify(inscriptionRepository).findById(1L);
        verify(eleveRepository).findById(2L);
        verifyNoInteractions(inscriptionMapper);

        verify(inscriptionRepository, never())
                .save(any(Inscription.class));
    }

    @Test
    void shouldDeleteInscription() {
        when(inscriptionRepository.findById(1L))
                .thenReturn(Optional.of(inscription));

        inscriptionService.delete(1L);

        verify(inscriptionRepository).findById(1L);
        verify(inscriptionRepository).delete(inscription);
    }

    @Test
    void shouldRejectDeletionWhenInscriptionDoesNotExist() {
        when(inscriptionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inscriptionService.delete(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "L'inscription ayant l'identifiant "
                                + "99 est introuvable."
                );

        verify(inscriptionRepository).findById(99L);

        verify(inscriptionRepository, never())
                .delete(any(Inscription.class));
    }
}