package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.ponderer.PondererResponse;
import fr.afpa.backend.entity.Ponderer;
import org.springframework.stereotype.Component;

@Component
public class PondererMapper {
    public PondererResponse toResponse(Ponderer p) {
        return new PondererResponse(p.getId().getIdClasse(), p.getId().getIdMatiere(), p.getCoefficientMatiere());
    }
}
