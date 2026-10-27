package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.responsable.ResponsableRequest;
import fr.afpa.backend.dto.responsable.ResponsableResponse;
import fr.afpa.backend.entity.Responsable;
import fr.afpa.backend.entity.Personne;
import org.springframework.stereotype.Component;

@Component
public class ResponsableMapper {

    public Responsable toEntity(
            ResponsableRequest request
    ) {
        return new Responsable(
                new Personne(request.nom(), request.prenom(), request.emailContact(),
                        request.telephone(), request.adresse()),
                request.profession()
        );
    }

    public ResponsableResponse toResponse(
            Responsable responsable
    ) {
        return new ResponsableResponse(
                responsable.getIdPersonne(),
                responsable.getPersonne().getNom(),
                responsable.getPersonne().getPrenom(),
                responsable.getPersonne().getEmailContact(),
                responsable.getPersonne().getTelephone(),
                responsable.getPersonne().getAdresse(),
                responsable.getProfession()
        );
    }

    public void updateEntity(
            ResponsableRequest request,
            Responsable responsable
    ) {
        responsable.getPersonne().setNom(request.nom());
        responsable.getPersonne().setPrenom(request.prenom());
        responsable.getPersonne().setEmailContact(request.emailContact());
        responsable.getPersonne().setTelephone(request.telephone());
        responsable.getPersonne().setAdresse(request.adresse());
        responsable.setProfession(request.profession());
    }
}
