package fr.afpa.backend.service;

import fr.afpa.backend.dto.periode.PeriodeRequest;
import fr.afpa.backend.dto.periode.PeriodeResponse;
import fr.afpa.backend.entity.Periode;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.PeriodeMapper;
import fr.afpa.backend.repository.PeriodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PeriodeService {

    private final PeriodeRepository periodeRepository;
    private final PeriodeMapper periodeMapper;

    public PeriodeService(
            PeriodeRepository periodeRepository,
            PeriodeMapper periodeMapper
    ) {
        this.periodeRepository = periodeRepository;
        this.periodeMapper = periodeMapper;
    }

    public List<PeriodeResponse> findAll() {
        return periodeRepository.findAll()
                .stream()
                .map(periodeMapper::toResponse)
                .toList();
    }

    public PeriodeResponse findById(Long id) {
        return periodeMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public PeriodeResponse create(PeriodeRequest request) {
        Periode periode = periodeMapper.toEntity(request);
        Periode savedPeriode = periodeRepository.save(periode);

        return periodeMapper.toResponse(savedPeriode);
    }

    @Transactional
    public PeriodeResponse update(
            Long id,
            PeriodeRequest request
    ) {
        Periode periode = findEntityById(id);

        periodeMapper.updateEntity(request, periode);

        Periode updatedPeriode =
                periodeRepository.save(periode);

        return periodeMapper.toResponse(updatedPeriode);
    }

    @Transactional
    public void delete(Long id) {
        Periode periode = findEntityById(id);
        periodeRepository.delete(periode);
    }

    private Periode findEntityById(Long id) {
        return periodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La période ayant l'identifiant "
                                + id
                                + " est introuvable."
                ));
    }
}