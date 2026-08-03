package fr.afpa.backend.service;

import fr.afpa.backend.dto.responsable.ResponsableRequest;
import fr.afpa.backend.dto.responsable.ResponsableResponse;
import fr.afpa.backend.entity.Responsable;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.ResponsableMapper;
import fr.afpa.backend.repository.ResponsableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ResponsableService {

    private final ResponsableRepository responsableRepository;
    private final ResponsableMapper responsableMapper;

    public ResponsableService(
            ResponsableRepository responsableRepository,
            ResponsableMapper responsableMapper
    ) {
        this.responsableRepository = responsableRepository;
        this.responsableMapper = responsableMapper;
    }

    public List<ResponsableResponse> findAll() {
        return responsableRepository.findAll()
                .stream()
                .map(responsableMapper::toResponse)
                .toList();
    }

    public ResponsableResponse findById(Long id) {
        return responsableMapper.toResponse(
                findEntityById(id)
        );
    }

    @Transactional
    public ResponsableResponse create(
            ResponsableRequest request
    ) {
        Responsable responsable =
                responsableMapper.toEntity(request);

        Responsable savedResponsable =
                responsableRepository.save(responsable);

        return responsableMapper.toResponse(
                savedResponsable
        );
    }

    @Transactional
    public ResponsableResponse update(
            Long id,
            ResponsableRequest request
    ) {
        Responsable responsable = findEntityById(id);

        responsableMapper.updateEntity(
                request,
                responsable
        );

        Responsable updatedResponsable =
                responsableRepository.save(responsable);

        return responsableMapper.toResponse(
                updatedResponsable
        );
    }

    @Transactional
    public void delete(Long id) {
        Responsable responsable = findEntityById(id);
        responsableRepository.delete(responsable);
    }

    private Responsable findEntityById(Long id) {
        return responsableRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Le responsable ayant l'identifiant "
                                        + id
                                        + " est introuvable."
                        )
                );
    }
}
