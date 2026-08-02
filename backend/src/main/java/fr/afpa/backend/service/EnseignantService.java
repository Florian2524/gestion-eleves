package fr.afpa.backend.service;

import fr.afpa.backend.dto.enseignant.EnseignantRequest;
import fr.afpa.backend.dto.enseignant.EnseignantResponse;
import fr.afpa.backend.entity.Enseignant;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceInUseException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.EnseignantMapper;
import fr.afpa.backend.repository.EnseignantRepository;
import fr.afpa.backend.repository.EnseignementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EnseignantService {

    private final EnseignantRepository enseignantRepository;
    private final EnseignementRepository enseignementRepository;
    private final EnseignantMapper enseignantMapper;

    public EnseignantService(
            EnseignantRepository enseignantRepository,
            EnseignementRepository enseignementRepository,
            EnseignantMapper enseignantMapper
    ) {
        this.enseignantRepository = enseignantRepository;
        this.enseignementRepository = enseignementRepository;
        this.enseignantMapper = enseignantMapper;
    }

    public List<EnseignantResponse> findAll() {
        return enseignantRepository.findAll()
                .stream()
                .map(enseignantMapper::toResponse)
                .toList();
    }

    public EnseignantResponse findById(Long id) {
        return enseignantMapper.toResponse(
                findEntityById(id)
        );
    }

    @Transactional
    public EnseignantResponse create(
            EnseignantRequest request
    ) {
        if (enseignantRepository.existsByNumeroEmploye(
                request.numeroEmploye()
        )) {
            throw new DuplicateResourceException(
                    "Un enseignant possède déjà le numéro d'employé "
                            + request.numeroEmploye()
                            + "."
            );
        }

        Enseignant enseignant =
                enseignantMapper.toEntity(request);

        Enseignant savedEnseignant =
                enseignantRepository.save(enseignant);

        return enseignantMapper.toResponse(savedEnseignant);
    }

    @Transactional
    public EnseignantResponse update(
            Long id,
            EnseignantRequest request
    ) {
        Enseignant enseignant = findEntityById(id);

        boolean numeroEmployeAlreadyUsed =
                enseignantRepository
                        .existsByNumeroEmployeAndIdPersonneNot(
                                request.numeroEmploye(),
                                id
                        );

        if (numeroEmployeAlreadyUsed) {
            throw new DuplicateResourceException(
                    "Un autre enseignant possède déjà le numéro d'employé "
                            + request.numeroEmploye()
                            + "."
            );
        }

        enseignantMapper.updateEntity(request, enseignant);

        Enseignant updatedEnseignant =
                enseignantRepository.save(enseignant);

        return enseignantMapper.toResponse(updatedEnseignant);
    }

    @Transactional
    public void delete(Long id) {
        Enseignant enseignant = findEntityById(id);

        if (enseignementRepository
                .existsByEnseignant_IdPersonne(id)) {
            throw new ResourceInUseException(
                    "Impossible de supprimer l'enseignant ayant l'identifiant "
                            + id
                            + " car il est associé à au moins un enseignement."
            );
        }

        enseignantRepository.delete(enseignant);
    }

    private Enseignant findEntityById(Long id) {
        return enseignantRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "L'enseignant ayant l'identifiant "
                                        + id
                                        + " est introuvable."
                        )
                );
    }
}