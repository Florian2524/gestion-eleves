package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.responsable.ResponsableRequest;
import fr.afpa.backend.dto.responsable.ResponsableResponse;
import fr.afpa.backend.entity.Responsable;
import org.springframework.stereotype.Component;

@Component
public class ResponsableMapper {

    public Responsable toEntity(
            ResponsableRequest request
    ) {
        return new Responsable(
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.profession()
        );
    }

    public ResponsableResponse toResponse(
            Responsable responsable
    ) {
        return new ResponsableResponse(
                responsable.getIdPersonne(),
                responsable.getNom(),
                responsable.getPrenom(),
                responsable.getEmailContact(),
                responsable.getTelephone(),
                responsable.getAdresse(),
                responsable.getProfession()
        );
    }

    public void updateEntity(
            ResponsableRequest request,
            Responsable responsable
    ) {
        responsable.setNom(request.nom());
        responsable.setPrenom(request.prenom());
        responsable.setEmailContact(request.emailContact());
        responsable.setTelephone(request.telephone());
        responsable.setAdresse(request.adresse());
        responsable.setProfession(request.profession());
    }
}
