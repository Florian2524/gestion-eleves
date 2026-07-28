package fr.afpa.backend.service;

import fr.afpa.backend.dto.matiere.MatiereRequest;
import fr.afpa.backend.dto.matiere.MatiereResponse;
import fr.afpa.backend.entity.Matiere;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.MatiereMapper;
import fr.afpa.backend.repository.MatiereRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MatiereService {

    private final MatiereRepository matiereRepository;
    private final MatiereMapper matiereMapper;

    public MatiereService(
            MatiereRepository matiereRepository,
            MatiereMapper matiereMapper
    ) {
        this.matiereRepository = matiereRepository;
        this.matiereMapper = matiereMapper;
    }

    public List<MatiereResponse> findAll() {
        return matiereRepository.findAll()
                .stream()
                .map(matiereMapper::toResponse)
                .toList();
    }

    public MatiereResponse findById(Long id) {
        return matiereMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public MatiereResponse create(MatiereRequest request) {
        if (matiereRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException(
                    "Une matière possède déjà le code "
                            + request.code()
                            + "."
            );
        }

        Matiere matiere = matiereMapper.toEntity(request);
        Matiere savedMatiere = matiereRepository.save(matiere);

        return matiereMapper.toResponse(savedMatiere);
    }

    @Transactional
    public MatiereResponse update(
            Long id,
            MatiereRequest request
    ) {
        Matiere matiere = findEntityById(id);

        boolean codeAlreadyUsed =
                matiereRepository.existsByCodeAndIdMatiereNot(
                        request.code(),
                        id
                );

        if (codeAlreadyUsed) {
            throw new DuplicateResourceException(
                    "Une autre matière possède déjà le code "
                            + request.code()
                            + "."
            );
        }

        matiereMapper.updateEntity(request, matiere);
        Matiere updatedMatiere =
                matiereRepository.save(matiere);

        return matiereMapper.toResponse(updatedMatiere);
    }

    @Transactional
    public void delete(Long id) {
        Matiere matiere = findEntityById(id);
        matiereRepository.delete(matiere);
    }

    private Matiere findEntityById(Long id) {
        return matiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La matière ayant l'identifiant "
                                + id
                                + " est introuvable."
                ));
    }
}