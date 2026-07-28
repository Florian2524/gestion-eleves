package fr.afpa.backend.service;

import fr.afpa.backend.dto.classe.ClasseRequest;
import fr.afpa.backend.dto.classe.ClasseResponse;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Enseignant;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.ClasseMapper;
import fr.afpa.backend.repository.ClasseRepository;
import fr.afpa.backend.repository.EnseignantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClasseService {

    private final ClasseRepository classeRepository;
    private final EnseignantRepository enseignantRepository;
    private final ClasseMapper classeMapper;

    public ClasseService(
            ClasseRepository classeRepository,
            EnseignantRepository enseignantRepository,
            ClasseMapper classeMapper
    ) {
        this.classeRepository = classeRepository;
        this.enseignantRepository = enseignantRepository;
        this.classeMapper = classeMapper;
    }

    public List<ClasseResponse> findAll() {
        return classeRepository.findAll()
                .stream()
                .map(classeMapper::toResponse)
                .toList();
    }

    public ClasseResponse findById(Long id) {
        return classeMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public ClasseResponse create(ClasseRequest request) {
        boolean classeAlreadyExists =
                classeRepository.existsByNomAndAnneeScolaire(
                        request.nom(),
                        request.anneeScolaire()
                );

        if (classeAlreadyExists) {
            throw new DuplicateResourceException(
                    "Une classe nommée "
                            + request.nom()
                            + " existe déjà pour l'année scolaire "
                            + request.anneeScolaire()
                            + "."
            );
        }

        Enseignant professeurPrincipal =
                findProfesseurPrincipal(request.idProfesseurPrincipal());

        Classe classe = classeMapper.toEntity(
                request,
                professeurPrincipal
        );

        Classe savedClasse = classeRepository.save(classe);

        return classeMapper.toResponse(savedClasse);
    }

    @Transactional
    public ClasseResponse update(
            Long id,
            ClasseRequest request
    ) {
        Classe classe = findEntityById(id);

        boolean classeAlreadyExists =
                classeRepository
                        .existsByNomAndAnneeScolaireAndIdClasseNot(
                                request.nom(),
                                request.anneeScolaire(),
                                id
                        );

        if (classeAlreadyExists) {
            throw new DuplicateResourceException(
                    "Une autre classe nommée "
                            + request.nom()
                            + " existe déjà pour l'année scolaire "
                            + request.anneeScolaire()
                            + "."
            );
        }

        Enseignant professeurPrincipal =
                findProfesseurPrincipal(request.idProfesseurPrincipal());

        classeMapper.updateEntity(
                request,
                professeurPrincipal,
                classe
        );

        Classe updatedClasse = classeRepository.save(classe);

        return classeMapper.toResponse(updatedClasse);
    }

    @Transactional
    public void delete(Long id) {
        Classe classe = findEntityById(id);
        classeRepository.delete(classe);
    }

    private Classe findEntityById(Long id) {
        return classeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La classe ayant l'identifiant "
                                + id
                                + " est introuvable."
                ));
    }

    private Enseignant findProfesseurPrincipal(
            Long idProfesseurPrincipal
    ) {
        if (idProfesseurPrincipal == null) {
            return null;
        }

        return enseignantRepository
                .findById(idProfesseurPrincipal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "L'enseignant ayant l'identifiant "
                                + idProfesseurPrincipal
                                + " est introuvable."
                ));
    }
}