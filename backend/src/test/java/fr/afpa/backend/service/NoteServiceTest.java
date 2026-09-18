package fr.afpa.backend.service;

import fr.afpa.backend.dto.note.NoteRequest;
import fr.afpa.backend.dto.note.NoteResponse;
import fr.afpa.backend.entity.Evaluation;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Enseignement;
import fr.afpa.backend.entity.Note;
import fr.afpa.backend.entity.Scolarite;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ForbiddenOperationException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.NoteMapper;
import fr.afpa.backend.repository.EvaluationRepository;
import fr.afpa.backend.repository.NoteRepository;
import fr.afpa.backend.repository.ScolariteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private ScolariteRepository scolariteRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private Scolarite scolarite;

    @Mock
    private Evaluation evaluation;
    @Mock private Classe classe;
    @Mock private Enseignement enseignement;

    private NoteService noteService;

    private NoteRequest request;
    private Note note;
    private NoteResponse response;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(scolarite.getClasse()).thenReturn(classe);
        org.mockito.Mockito.lenient().when(evaluation.getEnseignement()).thenReturn(enseignement);
        org.mockito.Mockito.lenient().when(enseignement.getClasse()).thenReturn(classe);
        org.mockito.Mockito.lenient().when(classe.getIdClasse()).thenReturn(1L);
        noteService = new NoteService(
                noteRepository,
                scolariteRepository,
                evaluationRepository,
                noteMapper
        );

        request = new NoteRequest(
                20L,
                100L,
                BigDecimal.valueOf(15.5),
                "Bon travail.",
                "SAISIE"
        );

        note = new Note(
                scolarite,
                evaluation,
                request.valeur(),
                request.commentaire(),
                request.statutNote()
        );

        response = new NoteResponse(
                200L,
                request.idScolarite(),
                request.idEvaluation(),
                request.valeur(),
                request.commentaire(),
                request.statutNote(),
                OffsetDateTime.parse(
                        "2026-10-15T14:30:00+02:00"
                )
        );
    }

    @Test
    void shouldReturnAllNotes() {
        when(noteRepository.findAll())
                .thenReturn(List.of(note));

        when(noteMapper.toResponse(note))
                .thenReturn(response);

        List<NoteResponse> result =
                noteService.findAll();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(noteRepository).findAll();
        verify(noteMapper).toResponse(note);
    }

    @Test
    void shouldReturnNoteById() {
        when(noteRepository.findById(200L))
                .thenReturn(Optional.of(note));

        when(noteMapper.toResponse(note))
                .thenReturn(response);

        NoteResponse result =
                noteService.findById(200L);

        assertThat(result).isEqualTo(response);

        verify(noteRepository).findById(200L);
        verify(noteMapper).toResponse(note);
    }

    @Test
    void shouldThrowExceptionWhenNoteDoesNotExist() {
        when(noteRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                noteService.findById(999L)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage(
                        "La note ayant l'identifiant "
                                + "999 est introuvable."
                );

        verify(noteRepository).findById(999L);
        verifyNoInteractions(noteMapper);
    }

    @Test
    void shouldCreateNote() {
        when(noteRepository
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluation(
                        request.idScolarite(),
                        request.idEvaluation()
                ))
                .thenReturn(false);

        when(scolariteRepository.findById(
                request.idScolarite()
        )).thenReturn(Optional.of(scolarite));

        when(evaluationRepository.findById(
                request.idEvaluation()
        )).thenReturn(Optional.of(evaluation));

        when(noteMapper.toEntity(
                request,
                scolarite,
                evaluation
        )).thenReturn(note);

        when(noteRepository.save(note))
                .thenReturn(note);

        when(noteMapper.toResponse(note))
                .thenReturn(response);

        NoteResponse result =
                noteService.create(request);

        assertThat(result).isEqualTo(response);

        verify(noteRepository)
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluation(
                        request.idScolarite(),
                        request.idEvaluation()
                );

        verify(scolariteRepository)
                .findById(request.idScolarite());

        verify(evaluationRepository)
                .findById(request.idEvaluation());

        verify(noteMapper).toEntity(
                request,
                scolarite,
                evaluation
        );

        verify(noteRepository).save(note);
        verify(noteMapper).toResponse(note);
    }

    @Test
    void shouldRejectNoteForAnotherClass() {
        Classe autreClasse = org.mockito.Mockito.mock(Classe.class);
        when(scolariteRepository.findById(request.idScolarite()))
                .thenReturn(Optional.of(scolarite));
        when(evaluationRepository.findById(request.idEvaluation()))
                .thenReturn(Optional.of(evaluation));
        when(enseignement.getClasse()).thenReturn(autreClasse);
        when(autreClasse.getIdClasse()).thenReturn(2L);

        assertThatThrownBy(() -> noteService.create(request))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(noteRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreationWhenNoteAlreadyExists() {
        when(noteRepository
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluation(
                        request.idScolarite(),
                        request.idEvaluation()
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                noteService.create(request)
        )
                .isInstanceOf(
                        DuplicateResourceException.class
                )
                .hasMessage(
                        "Une note existe déjà pour la scolarité "
                                + request.idScolarite()
                                + " et l'évaluation "
                                + request.idEvaluation()
                                + "."
                );

        verify(noteRepository)
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluation(
                        request.idScolarite(),
                        request.idEvaluation()
                );

        verifyNoInteractions(
                scolariteRepository,
                evaluationRepository,
                noteMapper
        );

        verify(noteRepository, never())
                .save(any(Note.class));
    }

    @Test
    void shouldRejectCreationWhenScolariteDoesNotExist() {
        when(noteRepository
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluation(
                        request.idScolarite(),
                        request.idEvaluation()
                ))
                .thenReturn(false);

        when(scolariteRepository.findById(
                request.idScolarite()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                noteService.create(request)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage(
                        "La scolarité ayant l'identifiant "
                                + request.idScolarite()
                                + " est introuvable."
                );

        verify(scolariteRepository)
                .findById(request.idScolarite());

        verifyNoInteractions(
                evaluationRepository,
                noteMapper
        );

        verify(noteRepository, never())
                .save(any(Note.class));
    }

    @Test
    void shouldRejectCreationWhenEvaluationDoesNotExist() {
        when(noteRepository
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluation(
                        request.idScolarite(),
                        request.idEvaluation()
                ))
                .thenReturn(false);

        when(scolariteRepository.findById(
                request.idScolarite()
        )).thenReturn(Optional.of(scolarite));

        when(evaluationRepository.findById(
                request.idEvaluation()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                noteService.create(request)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage(
                        "L'évaluation ayant l'identifiant "
                                + request.idEvaluation()
                                + " est introuvable."
                );

        verify(scolariteRepository)
                .findById(request.idScolarite());

        verify(evaluationRepository)
                .findById(request.idEvaluation());

        verifyNoInteractions(noteMapper);

        verify(noteRepository, never())
                .save(any(Note.class));
    }

    @Test
    void shouldUpdateNote() {
        when(noteRepository.findById(200L))
                .thenReturn(Optional.of(note));

        when(noteRepository
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluationAndIdNoteNot(
                        request.idScolarite(),
                        request.idEvaluation(),
                        200L
                ))
                .thenReturn(false);

        when(scolariteRepository.findById(
                request.idScolarite()
        )).thenReturn(Optional.of(scolarite));

        when(evaluationRepository.findById(
                request.idEvaluation()
        )).thenReturn(Optional.of(evaluation));

        when(noteRepository.save(note))
                .thenReturn(note);

        when(noteMapper.toResponse(note))
                .thenReturn(response);

        NoteResponse result =
                noteService.update(
                        200L,
                        request
                );

        assertThat(result).isEqualTo(response);

        verify(noteRepository).findById(200L);

        verify(noteRepository)
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluationAndIdNoteNot(
                        request.idScolarite(),
                        request.idEvaluation(),
                        200L
                );

        verify(scolariteRepository)
                .findById(request.idScolarite());

        verify(evaluationRepository)
                .findById(request.idEvaluation());

        verify(noteMapper).updateEntity(
                request,
                scolarite,
                evaluation,
                note
        );

        verify(noteRepository).save(note);
        verify(noteMapper).toResponse(note);
    }

    @Test
    void shouldRejectUpdateWhenNoteDoesNotExist() {
        when(noteRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                noteService.update(
                        999L,
                        request
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage(
                        "La note ayant l'identifiant "
                                + "999 est introuvable."
                );

        verify(noteRepository).findById(999L);

        verifyNoInteractions(
                scolariteRepository,
                evaluationRepository,
                noteMapper
        );

        verify(noteRepository, never())
                .save(any(Note.class));
    }

    @Test
    void shouldRejectUpdateWhenAnotherNoteAlreadyExists() {
        when(noteRepository.findById(200L))
                .thenReturn(Optional.of(note));

        when(noteRepository
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluationAndIdNoteNot(
                        request.idScolarite(),
                        request.idEvaluation(),
                        200L
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                noteService.update(
                        200L,
                        request
                )
        )
                .isInstanceOf(
                        DuplicateResourceException.class
                )
                .hasMessage(
                        "Une autre note existe déjà pour la scolarité "
                                + request.idScolarite()
                                + " et l'évaluation "
                                + request.idEvaluation()
                                + "."
                );

        verify(noteRepository).findById(200L);

        verify(noteRepository)
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluationAndIdNoteNot(
                        request.idScolarite(),
                        request.idEvaluation(),
                        200L
                );

        verifyNoInteractions(
                scolariteRepository,
                evaluationRepository,
                noteMapper
        );

        verify(noteRepository, never())
                .save(any(Note.class));
    }

    @Test
    void shouldRejectUpdateWhenScolariteDoesNotExist() {
        when(noteRepository.findById(200L))
                .thenReturn(Optional.of(note));

        when(noteRepository
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluationAndIdNoteNot(
                        request.idScolarite(),
                        request.idEvaluation(),
                        200L
                ))
                .thenReturn(false);

        when(scolariteRepository.findById(
                request.idScolarite()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                noteService.update(
                        200L,
                        request
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage(
                        "La scolarité ayant l'identifiant "
                                + request.idScolarite()
                                + " est introuvable."
                );

        verify(scolariteRepository)
                .findById(request.idScolarite());

        verifyNoInteractions(
                evaluationRepository,
                noteMapper
        );

        verify(noteRepository, never())
                .save(any(Note.class));
    }

    @Test
    void shouldRejectUpdateWhenEvaluationDoesNotExist() {
        when(noteRepository.findById(200L))
                .thenReturn(Optional.of(note));

        when(noteRepository
                .existsByScolarite_IdScolariteAndEvaluation_IdEvaluationAndIdNoteNot(
                        request.idScolarite(),
                        request.idEvaluation(),
                        200L
                ))
                .thenReturn(false);

        when(scolariteRepository.findById(
                request.idScolarite()
        )).thenReturn(Optional.of(scolarite));

        when(evaluationRepository.findById(
                request.idEvaluation()
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                noteService.update(
                        200L,
                        request
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage(
                        "L'évaluation ayant l'identifiant "
                                + request.idEvaluation()
                                + " est introuvable."
                );

        verify(scolariteRepository)
                .findById(request.idScolarite());

        verify(evaluationRepository)
                .findById(request.idEvaluation());

        verifyNoInteractions(noteMapper);

        verify(noteRepository, never())
                .save(any(Note.class));
    }

    @Test
    void shouldDeleteNote() {
        when(noteRepository.findById(200L))
                .thenReturn(Optional.of(note));

        noteService.delete(200L);

        verify(noteRepository).findById(200L);
        verify(noteRepository).delete(note);
    }

    @Test
    void shouldRejectDeletionWhenNoteDoesNotExist() {
        when(noteRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                noteService.delete(999L)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage(
                        "La note ayant l'identifiant "
                                + "999 est introuvable."
                );

        verify(noteRepository).findById(999L);

        verify(noteRepository, never())
                .delete(any(Note.class));
    }
}
