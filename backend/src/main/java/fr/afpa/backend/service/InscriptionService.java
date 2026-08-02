package fr.afpa.backend.service;

import fr.afpa.backend.dto.inscription.InscriptionRequest;
import fr.afpa.backend.dto.inscription.InscriptionResponse;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.Inscription;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.InscriptionMapper;
import fr.afpa.backend.repository.EleveRepository;
import fr.afpa.backend.repository.InscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final EleveRepository eleveRepository;
    private final InscriptionMapper inscriptionMapper;

    public InscriptionService(
            InscriptionRepository inscriptionRepository,
            EleveRepository eleveRepository,
            InscriptionMapper inscriptionMapper
    ) {
        this.inscriptionRepository = inscriptionRepository;
        this.eleveRepository = eleveRepository;
        this.inscriptionMapper = inscriptionMapper;
    }

    public List<InscriptionResponse> findAll() {
        return inscriptionRepository.findAll()
                .stream()
                .map(inscriptionMapper::toResponse)
                .toList();
    }

    public InscriptionResponse findById(Long id) {
        return inscriptionMapper.toResponse(
                findEntityById(id)
        );
    }

    @Transactional
    public InscriptionResponse create(
            InscriptionRequest request
    ) {
        Eleve eleve = findEleveById(request.idEleve());

        Inscription inscription = inscriptionMapper.toEntity(
                request,
                eleve
        );

        Inscription savedInscription =
                inscriptionRepository.save(inscription);

        return inscriptionMapper.toResponse(savedInscription);
    }

    @Transactional
    public InscriptionResponse update(
            Long id,
            InscriptionRequest request
    ) {
        Inscription inscription = findEntityById(id);
        Eleve eleve = findEleveById(request.idEleve());

        inscriptionMapper.updateEntity(
                request,
                eleve,
                inscription
        );

        Inscription updatedInscription =
                inscriptionRepository.save(inscription);

        return inscriptionMapper.toResponse(
                updatedInscription
        );
    }

    @Transactional
    public void delete(Long id) {
        Inscription inscription = findEntityById(id);
        inscriptionRepository.delete(inscription);
    }

    private Inscription findEntityById(Long id) {
        return inscriptionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "L'inscription ayant l'identifiant "
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
}