package fr.afpa.backend.service;

import fr.afpa.backend.dto.responsabilitelegale.ResponsabiliteLegaleRequest;
import fr.afpa.backend.dto.responsabilitelegale.ResponsabiliteLegaleResponse;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.ResponsabiliteLegale;
import fr.afpa.backend.entity.ResponsabiliteLegaleId;
import fr.afpa.backend.entity.Responsable;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.ResponsabiliteLegaleMapper;
import fr.afpa.backend.repository.EleveRepository;
import fr.afpa.backend.repository.ResponsabiliteLegaleRepository;
import fr.afpa.backend.repository.ResponsableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ResponsabiliteLegaleService {

    private final ResponsabiliteLegaleRepository
            responsabiliteLegaleRepository;

    private final ResponsableRepository responsableRepository;
    private final EleveRepository eleveRepository;

    private final ResponsabiliteLegaleMapper
            responsabiliteLegaleMapper;

    public ResponsabiliteLegaleService(
            ResponsabiliteLegaleRepository responsabiliteLegaleRepository,
            ResponsableRepository responsableRepository,
            EleveRepository eleveRepository,
            ResponsabiliteLegaleMapper responsabiliteLegaleMapper
    ) {
        this.responsabiliteLegaleRepository =
                responsabiliteLegaleRepository;

        this.responsableRepository = responsableRepository;
        this.eleveRepository = eleveRepository;

        this.responsabiliteLegaleMapper =
                responsabiliteLegaleMapper;
    }

    public List<ResponsabiliteLegaleResponse> findAll() {
        return responsabiliteLegaleRepository.findAll()
                .stream()
                .map(responsabiliteLegaleMapper::toResponse)
                .toList();
    }

    public ResponsabiliteLegaleResponse findById(
            Long idResponsable,
            Long idEleve
    ) {
        return responsabiliteLegaleMapper.toResponse(
                findEntityById(
                        idResponsable,
                        idEleve
                )
        );
    }

    @Transactional
    public ResponsabiliteLegaleResponse create(
            ResponsabiliteLegaleRequest request
    ) {
        ResponsabiliteLegaleId id =
                new ResponsabiliteLegaleId(
                        request.idResponsable(),
                        request.idEleve()
                );

        if (responsabiliteLegaleRepository.existsById(id)) {
            throw new DuplicateResourceException(
                    "La responsabilité légale entre le responsable "
                            + request.idResponsable()
                            + " et l'élève "
                            + request.idEleve()
                            + " existe déjà."
            );
        }

        Responsable responsable =
                responsableRepository
                        .findById(request.idResponsable())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Le responsable ayant l'identifiant "
                                                + request.idResponsable()
                                                + " est introuvable."
                                )
                        );

        Eleve eleve =
                eleveRepository
                        .findById(request.idEleve())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "L'élève ayant l'identifiant "
                                                + request.idEleve()
                                                + " est introuvable."
                                )
                        );

        ResponsabiliteLegale responsabiliteLegale =
                responsabiliteLegaleMapper.toEntity(
                        responsable,
                        eleve
                );

        ResponsabiliteLegale savedResponsabiliteLegale =
                responsabiliteLegaleRepository.save(
                        responsabiliteLegale
                );

        return responsabiliteLegaleMapper.toResponse(
                savedResponsabiliteLegale
        );
    }

    @Transactional
    public void delete(
            Long idResponsable,
            Long idEleve
    ) {
        ResponsabiliteLegale responsabiliteLegale =
                findEntityById(
                        idResponsable,
                        idEleve
                );

        responsabiliteLegaleRepository.delete(
                responsabiliteLegale
        );
    }

    private ResponsabiliteLegale findEntityById(
            Long idResponsable,
            Long idEleve
    ) {
        ResponsabiliteLegaleId id =
                new ResponsabiliteLegaleId(
                        idResponsable,
                        idEleve
                );

        return responsabiliteLegaleRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La responsabilité légale entre le responsable "
                                        + idResponsable
                                        + " et l'élève "
                                        + idEleve
                                        + " est introuvable."
                        )
                );
    }
}
