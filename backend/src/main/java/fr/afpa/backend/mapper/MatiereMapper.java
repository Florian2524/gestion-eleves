package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.matiere.MatiereRequest;
import fr.afpa.backend.dto.matiere.MatiereResponse;
import fr.afpa.backend.entity.Matiere;
import org.springframework.stereotype.Component;

@Component
public class MatiereMapper {

    public Matiere toEntity(MatiereRequest request) {
        return new Matiere(
                request.code(),
                request.nom()
        );
    }

    public MatiereResponse toResponse(Matiere matiere) {
        return new MatiereResponse(
                matiere.getIdMatiere(),
                matiere.getCode(),
                matiere.getNom()
        );
    }

    public void updateEntity(
            MatiereRequest request,
            Matiere matiere
    ) {
        matiere.setCode(request.code());
        matiere.setNom(request.nom());
    }
}