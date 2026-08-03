package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.responsabilitelegale.ResponsabiliteLegaleResponse;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.ResponsabiliteLegale;
import fr.afpa.backend.entity.Responsable;
import org.springframework.stereotype.Component;

@Component
public class ResponsabiliteLegaleMapper {

    public ResponsabiliteLegale toEntity(
            Responsable responsable,
            Eleve eleve
    ) {
        return new ResponsabiliteLegale(
                responsable,
                eleve
        );
    }

    public ResponsabiliteLegaleResponse toResponse(
            ResponsabiliteLegale responsabiliteLegale
    ) {
        Responsable responsable =
                responsabiliteLegale.getResponsable();

        Eleve eleve =
                responsabiliteLegale.getEleve();

        return new ResponsabiliteLegaleResponse(
                responsable.getIdPersonne(),
                responsable.getNom(),
                responsable.getPrenom(),
                eleve.getIdPersonne(),
                eleve.getNom(),
                eleve.getPrenom()
        );
    }
}
