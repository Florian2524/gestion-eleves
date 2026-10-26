package fr.afpa.backend.service;

import fr.afpa.backend.dto.ponderer.*;
import fr.afpa.backend.entity.*;
import fr.afpa.backend.exception.*;
import fr.afpa.backend.mapper.PondererMapper;
import fr.afpa.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PondererService {
    private final PondererRepository repository;
    private final ClasseRepository classes;
    private final MatiereRepository matieres;
    private final EnseignementRepository enseignements;
    private final PondererMapper mapper;

    public PondererService(PondererRepository repository, ClasseRepository classes, MatiereRepository matieres,
                          EnseignementRepository enseignements, PondererMapper mapper) {
        this.repository = repository;
        this.classes = classes;
        this.matieres = matieres;
        this.enseignements = enseignements;
        this.mapper = mapper;
    }
    public List<PondererResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }
    public PondererResponse findById(Long idClasse, Long idMatiere) {
        return mapper.toResponse(find(new PondererId(idClasse, idMatiere)));
    }
    @Transactional
    public PondererResponse create(PondererRequest request) {
        validateReferences(request);
        if (repository.insertIfAbsent(request.idClasse(), request.idMatiere(), request.coefficientMatiere()) == 0) {
            throw new DuplicateResourceException("Une pondération existe déjà pour ce couple classe-matière.");
        }
        return findById(request.idClasse(), request.idMatiere());
    }
    @Transactional
    public PondererResponse update(Long idClasse, Long idMatiere, PondererRequest request) {
        if (!idClasse.equals(request.idClasse()) || !idMatiere.equals(request.idMatiere())) {
            throw new BusinessRuleViolationException("La clé classe-matière est immuable.");
        }
        if (repository.updateCoefficient(new PondererId(idClasse, idMatiere), request.coefficientMatiere()) == 0) {
            throw absent(idClasse, idMatiere);
        }
        return findById(idClasse, idMatiere);
    }
    @Transactional
    public void delete(Long idClasse, Long idMatiere) {
        Ponderer p = find(new PondererId(idClasse, idMatiere));
        if (enseignements.existsByClasse_IdClasseAndMatiere_IdMatiere(idClasse, idMatiere)) {
            throw new ResourceInUseException("Cette pondération est utilisée par au moins un enseignement.");
        }
        repository.delete(p);
        repository.flush();
    }
    /** Contrat transitoire : initialiser ou vérifier, jamais changer le coefficient commun. */
    @Transactional
    public void ensureCompatible(Long idClasse, Long idMatiere, BigDecimal coefficient) {
        repository.insertIfAbsent(idClasse, idMatiere, coefficient);
        if (repository.findForShare(new PondererId(idClasse, idMatiere))
                .orElseThrow(() -> absent(idClasse, idMatiere)).getCoefficientMatiere().compareTo(coefficient) != 0) {
            throw new DuplicateResourceException("Coefficient différent de PONDERER : modifiez la pondération classe-matière explicitement.");
        }
    }
    private void validateReferences(PondererRequest request) {
        if (!classes.existsById(request.idClasse())) throw new ResourceNotFoundException("Classe introuvable.");
        if (!matieres.existsById(request.idMatiere())) throw new ResourceNotFoundException("Matière introuvable.");
    }
    private Ponderer find(PondererId id) {
        return repository.findById(id).orElseThrow(() -> absent(id.getIdClasse(), id.getIdMatiere()));
    }
    private ResourceNotFoundException absent(Long idClasse, Long idMatiere) {
        return new ResourceNotFoundException("Pondération absente pour la classe " + idClasse + " et la matière " + idMatiere + ".");
    }
}
