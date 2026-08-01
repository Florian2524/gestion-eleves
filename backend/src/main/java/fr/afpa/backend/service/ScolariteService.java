package fr.afpa.backend.service;

import fr.afpa.backend.dto.scolarite.ScolariteRequest;
import fr.afpa.backend.dto.scolarite.ScolariteResponse;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.Scolarite;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.ScolariteMapper;
import fr.afpa.backend.repository.ClasseRepository;
import fr.afpa.backend.repository.EleveRepository;
import fr.afpa.backend.repository.ScolariteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ScolariteService {

    private final ScolariteRepository scolariteRepository;
    private final EleveRepository eleveRepository;
    private final ClasseRepository classeRepository;
    private final ScolariteMapper scolariteMapper;

    public ScolariteService(
            ScolariteRepository scolariteRepository,
            EleveRepository eleveRepository,
            ClasseRepository classeRepository,
            ScolariteMapper scolariteMapper
    ) {
        this.scolariteRepository = scolariteRepository;
        this.eleveRepository = eleveRepository;
        this.classeRepository = classeRepository;
        this.scolariteMapper = scolariteMapper;
    }

    public List<ScolariteResponse> findAll() {
        return scolariteRepository.findAll()
                .stream()
                .map(scolariteMapper::toResponse)
                .toList();
    }

    public ScolariteResponse findById(Long id) {
        return scolariteMapper.toResponse(
                findEntityById(id)
        );
    }

    @Transactional
    public ScolariteResponse create(
            ScolariteRequest request
    ) {
        boolean scolariteAlreadyExists =
                scolariteRepository
                        .existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebut(
                                request.idEleve(),
                                request.idClasse(),
                                request.dateDebut()
                        );

        if (scolariteAlreadyExists) {
            throw new DuplicateResourceException(
                    "Une scolarité existe déjà pour l'élève "
                            + request.idEleve()
                            + ", la classe "
                            + request.idClasse()
                            + " et la date de début "
                            + request.dateDebut()
                            + "."
            );
        }

        Eleve eleve = findEleveById(request.idEleve());
        Classe classe = findClasseById(request.idClasse());

        Scolarite scolarite = scolariteMapper.toEntity(
                request,
                eleve,
                classe
        );

        Scolarite savedScolarite =
                scolariteRepository.save(scolarite);

        return scolariteMapper.toResponse(savedScolarite);
    }

    @Transactional
    public ScolariteResponse update(
            Long id,
            ScolariteRequest request
    ) {
        Scolarite scolarite = findEntityById(id);

        boolean scolariteAlreadyExists =
                scolariteRepository
                        .existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebutAndIdScolariteNot(
                                request.idEleve(),
                                request.idClasse(),
                                request.dateDebut(),
                                id
                        );

        if (scolariteAlreadyExists) {
            throw new DuplicateResourceException(
                    "Une autre scolarité existe déjà pour l'élève "
                            + request.idEleve()
                            + ", la classe "
                            + request.idClasse()
                            + " et la date de début "
                            + request.dateDebut()
                            + "."
            );
        }

        Eleve eleve = findEleveById(request.idEleve());
        Classe classe = findClasseById(request.idClasse());

        scolariteMapper.updateEntity(
                request,
                eleve,
                classe,
                scolarite
        );

        Scolarite updatedScolarite =
                scolariteRepository.save(scolarite);

        return scolariteMapper.toResponse(updatedScolarite);
    }

    @Transactional
    public void delete(Long id) {
        Scolarite scolarite = findEntityById(id);
        scolariteRepository.delete(scolarite);
    }

    private Scolarite findEntityById(Long id) {
        return scolariteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La scolarité ayant l'identifiant "
                                        + id
                                        + " est introuvable."
                        )
                );
    }

    private Eleve findEleveById(Long idEleve) {
        return eleveRepository.findById(idEleve)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "L'élève ayant l'identifiant "
                                        + idEleve
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
}