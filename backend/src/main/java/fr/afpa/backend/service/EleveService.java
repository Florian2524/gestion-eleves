package fr.afpa.backend.service;

import fr.afpa.backend.dto.eleve.EleveRequest;
import fr.afpa.backend.dto.eleve.EleveResponse;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.EleveMapper;
import fr.afpa.backend.repository.EleveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EleveService {

    private final EleveRepository eleveRepository;
    private final EleveMapper eleveMapper;

    public EleveService(
            EleveRepository eleveRepository,
            EleveMapper eleveMapper
    ) {
        this.eleveRepository = eleveRepository;
        this.eleveMapper = eleveMapper;
    }

    public List<EleveResponse> findAll() {
        return eleveRepository.findAll()
                .stream()
                .map(eleveMapper::toResponse)
                .toList();
    }

    public EleveResponse findById(Long id) {
        return eleveMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public EleveResponse create(EleveRequest request) {
        if (eleveRepository.existsByMatricule(request.matricule())) {
            throw new DuplicateResourceException(
                    "Un élève possède déjà le matricule "
                            + request.matricule()
                            + "."
            );
        }

        Eleve eleve = eleveMapper.toEntity(request);
        Eleve savedEleve = eleveRepository.save(eleve);

        return eleveMapper.toResponse(savedEleve);
    }

    @Transactional
    public EleveResponse update(Long id, EleveRequest request) {
        Eleve eleve = findEntityById(id);

        boolean matriculeAlreadyUsed =
                eleveRepository.existsByMatriculeAndIdPersonneNot(
                        request.matricule(),
                        id
                );

        if (matriculeAlreadyUsed) {
            throw new DuplicateResourceException(
                    "Un autre élève possède déjà le matricule "
                            + request.matricule()
                            + "."
            );
        }

        eleveMapper.updateEntity(request, eleve);
        Eleve updatedEleve = eleveRepository.save(eleve);

        return eleveMapper.toResponse(updatedEleve);
    }

    @Transactional
    public void delete(Long id) {
        Eleve eleve = findEntityById(id);
        eleveRepository.delete(eleve);
    }

    private Eleve findEntityById(Long id) {
        return eleveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "L'élève ayant l'identifiant "
                                + id
                                + " est introuvable."
                ));
    }
}