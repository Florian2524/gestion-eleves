package fr.afpa.backend.service;

import fr.afpa.backend.dto.note.NoteRequest;
import fr.afpa.backend.dto.note.NoteResponse;
import fr.afpa.backend.entity.Evaluation;
import fr.afpa.backend.entity.Note;
import fr.afpa.backend.entity.Scolarite;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ForbiddenOperationException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.NoteMapper;
import fr.afpa.backend.repository.EvaluationRepository;
import fr.afpa.backend.repository.NoteRepository;
import fr.afpa.backend.repository.ScolariteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;
    private final ScolariteRepository scolariteRepository;
    private final EvaluationRepository evaluationRepository;
    private final NoteMapper noteMapper;

    public NoteService(
            NoteRepository noteRepository,
            ScolariteRepository scolariteRepository,
            EvaluationRepository evaluationRepository,
            NoteMapper noteMapper
    ) {
        this.noteRepository = noteRepository;
        this.scolariteRepository = scolariteRepository;
        this.evaluationRepository = evaluationRepository;
        this.noteMapper = noteMapper;
    }

    public List<NoteResponse> findAll() {
        return noteRepository.findAll()
                .stream()
                .map(noteMapper::toResponse)
                .toList();
    }

    public NoteResponse findById(Long id) {
        return noteMapper.toResponse(
                findEntityById(id)
        );
    }

    @Transactional
    public NoteResponse create(NoteRequest request) {
        boolean noteAlreadyExists =
                noteRepository
                        .existsByScolarite_IdScolariteAndEvaluation_IdEvaluation(
                                request.idScolarite(),
                                request.idEvaluation()
                        );

        if (noteAlreadyExists) {
            throw new DuplicateResourceException(
                    "Une note existe déjà pour la scolarité "
                            + request.idScolarite()
                            + " et l'évaluation "
                            + request.idEvaluation()
                            + "."
            );
        }

        Scolarite scolarite =
                findScolariteById(request.idScolarite());

        Evaluation evaluation =
                findEvaluationById(request.idEvaluation());

        verifierClasse(scolarite, evaluation);
        verifierBareme(request, evaluation);

        Note note = noteMapper.toEntity(
                request,
                scolarite,
                evaluation
        );

        Note savedNote = noteRepository.save(note);

        return noteMapper.toResponse(savedNote);
    }

    @Transactional
    public NoteResponse update(
            Long id,
            NoteRequest request
    ) {
        Note note = findEntityById(id);

        boolean noteAlreadyExists =
                noteRepository
                        .existsByScolarite_IdScolariteAndEvaluation_IdEvaluationAndIdNoteNot(
                                request.idScolarite(),
                                request.idEvaluation(),
                                id
                        );

        if (noteAlreadyExists) {
            throw new DuplicateResourceException(
                    "Une autre note existe déjà pour la scolarité "
                            + request.idScolarite()
                            + " et l'évaluation "
                            + request.idEvaluation()
                            + "."
            );
        }

        Scolarite scolarite =
                findScolariteById(request.idScolarite());

        Evaluation evaluation =
                findEvaluationById(request.idEvaluation());

        verifierClasse(scolarite, evaluation);
        verifierBareme(request, evaluation);

        noteMapper.updateEntity(
                request,
                scolarite,
                evaluation,
                note
        );

        Note updatedNote = noteRepository.save(note);

        return noteMapper.toResponse(updatedNote);
    }

    @Transactional
    public void delete(Long id) {
        Note note = findEntityById(id);
        noteRepository.delete(note);
    }

    private Note findEntityById(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La note ayant l'identifiant "
                                        + id
                                        + " est introuvable."
                        )
                );
    }

    private Scolarite findScolariteById(
            Long idScolarite
    ) {
        return scolariteRepository.findById(idScolarite)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La scolarité ayant l'identifiant "
                                        + idScolarite
                                        + " est introuvable."
                        )
                );
    }

    private Evaluation findEvaluationById(
            Long idEvaluation
    ) {
        return evaluationRepository.findById(idEvaluation)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "L'évaluation ayant l'identifiant "
                                        + idEvaluation
                                        + " est introuvable."
                        )
                );
    }

    private void verifierClasse(Scolarite scolarite, Evaluation evaluation) {
        if (!scolarite.getClasse().getIdClasse().equals(
                evaluation.getEnseignement().getClasse().getIdClasse())) {
            throw new ForbiddenOperationException(
                    "La scolarité ne concerne pas la classe de l'évaluation."
            );
        }
    }

    private void verifierBareme(NoteRequest request, Evaluation evaluation) {
        if (request.valeur().compareTo(evaluation.getBareme()) > 0) {
            throw new ForbiddenOperationException(
                    "La note ne peut pas dépasser le barème de l'évaluation."
            );
        }
    }
}
