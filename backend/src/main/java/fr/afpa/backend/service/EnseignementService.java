package fr.afpa.backend.service;

import fr.afpa.backend.dto.enseignement.EnseignementRequest;
import fr.afpa.backend.dto.enseignement.EnseignementResponse;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Enseignant;
import fr.afpa.backend.entity.Enseignement;
import fr.afpa.backend.entity.Matiere;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceInUseException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.EnseignementMapper;
import fr.afpa.backend.repository.ClasseRepository;
import fr.afpa.backend.repository.EnseignantRepository;
import fr.afpa.backend.repository.EnseignementRepository;
import fr.afpa.backend.repository.EvaluationRepository;
import fr.afpa.backend.repository.LigneBulletinRepository;
import fr.afpa.backend.repository.MatiereRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EnseignementService {

    private final EnseignementRepository enseignementRepository;
    private final EnseignantRepository enseignantRepository;
    private final ClasseRepository classeRepository;
    private final MatiereRepository matiereRepository;
    private final EvaluationRepository evaluationRepository;
    private final LigneBulletinRepository ligneBulletinRepository;
    private final EnseignementMapper enseignementMapper;

    public EnseignementService(
            EnseignementRepository enseignementRepository,
            EnseignantRepository enseignantRepository,
            ClasseRepository classeRepository,
            MatiereRepository matiereRepository,
            EvaluationRepository evaluationRepository,
            LigneBulletinRepository ligneBulletinRepository,
            EnseignementMapper enseignementMapper
    ) {
        this.enseignementRepository = enseignementRepository;
        this.enseignantRepository = enseignantRepository;
        this.classeRepository = classeRepository;
        this.matiereRepository = matiereRepository;
        this.evaluationRepository = evaluationRepository;
        this.ligneBulletinRepository = ligneBulletinRepository;
        this.enseignementMapper = enseignementMapper;
    }

    public List<EnseignementResponse> findAll() {
        return enseignementRepository.findAll()
                .stream()
                .map(enseignementMapper::toResponse)
                .toList();
    }

    public EnseignementResponse findById(Long id) {
        return enseignementMapper.toResponse(
                findEntityById(id)
        );
    }

    @Transactional
    public EnseignementResponse create(
            EnseignementRequest request
    ) {
        boolean enseignementAlreadyExists =
                enseignementRepository
                        .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebut(
                                request.idEnseignant(),
                                request.idClasse(),
                                request.idMatiere(),
                                request.dateDebut()
                        );

        if (enseignementAlreadyExists) {
            throw new DuplicateResourceException(
                    "Un enseignement existe déjà pour l'enseignant "
                            + request.idEnseignant()
                            + ", la classe "
                            + request.idClasse()
                            + ", la matière "
                            + request.idMatiere()
                            + " et la date de début "
                            + request.dateDebut()
                            + "."
            );
        }

        Enseignant enseignant =
                findEnseignantById(request.idEnseignant());

        Classe classe =
                findClasseById(request.idClasse());

        Matiere matiere =
                findMatiereById(request.idMatiere());

        Enseignement enseignement =
                enseignementMapper.toEntity(
                        request,
                        enseignant,
                        classe,
                        matiere
                );

        Enseignement savedEnseignement =
                enseignementRepository.save(enseignement);

        return enseignementMapper.toResponse(
                savedEnseignement
        );
    }

    @Transactional
    public EnseignementResponse update(
            Long id,
            EnseignementRequest request
    ) {
        Enseignement enseignement =
                findEntityById(id);

        boolean enseignementAlreadyExists =
                enseignementRepository
                        .existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebutAndIdEnseignementNot(
                                request.idEnseignant(),
                                request.idClasse(),
                                request.idMatiere(),
                                request.dateDebut(),
                                id
                        );

        if (enseignementAlreadyExists) {
            throw new DuplicateResourceException(
                    "Un autre enseignement existe déjà pour l'enseignant "
                            + request.idEnseignant()
                            + ", la classe "
                            + request.idClasse()
                            + ", la matière "
                            + request.idMatiere()
                            + " et la date de début "
                            + request.dateDebut()
                            + "."
            );
        }

        Enseignant enseignant =
                findEnseignantById(request.idEnseignant());

        Classe classe =
                findClasseById(request.idClasse());

        Matiere matiere =
                findMatiereById(request.idMatiere());

        enseignementMapper.updateEntity(
                request,
                enseignant,
                classe,
                matiere,
                enseignement
        );

        Enseignement updatedEnseignement =
                enseignementRepository.save(enseignement);

        return enseignementMapper.toResponse(
                updatedEnseignement
        );
    }

    @Transactional
    public void delete(Long id) {
        Enseignement enseignement =
                findEntityById(id);

        boolean usedByEvaluation =
                evaluationRepository
                        .existsByEnseignement_IdEnseignement(id);

        boolean usedByBulletinLine =
                ligneBulletinRepository
                        .existsByEnseignement_IdEnseignement(id);

        if (usedByEvaluation || usedByBulletinLine) {
            throw new ResourceInUseException(
                    "Impossible de supprimer l'enseignement ayant l'identifiant "
                            + id
                            + " car il est associé à au moins une évaluation ou une ligne de bulletin."
            );
        }

        enseignementRepository.delete(enseignement);
    }

    private Enseignement findEntityById(Long id) {
        return enseignementRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "L'enseignement ayant l'identifiant "
                                        + id
                                        + " est introuvable."
                        )
                );
    }

    private Enseignant findEnseignantById(
            Long idEnseignant
    ) {
        return enseignantRepository.findById(idEnseignant)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "L'enseignant ayant l'identifiant "
                                        + idEnseignant
                                        + " est introuvable."
                        )
                );
    }

    private Classe findClasseById(Long idClasse) {
        return classeRepository.findById(idClasse)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La classe ayant l'identifiant "
                                        + idClasse
                                        + " est introuvable."
                        )
                );
    }

    private Matiere findMatiereById(Long idMatiere) {
        return matiereRepository.findById(idMatiere)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La matière ayant l'identifiant "
                                        + idMatiere
                                        + " est introuvable."
                        )
                );
    }
}