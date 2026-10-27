package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.eleve.EleveRequest;
import fr.afpa.backend.dto.eleve.EleveResponse;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.Personne;
import org.springframework.stereotype.Component;

@Component
public class EleveMapper {

    public Eleve toEntity(EleveRequest request) {
        return new Eleve(
                new Personne(request.nom(), request.prenom(), request.emailContact(),
                        request.telephone(), request.adresse()),
                request.matricule(),
                request.dateNaissance(),
                null
        );
    }

    public EleveResponse toResponse(Eleve eleve) {
        return new EleveResponse(
                eleve.getIdPersonne(),
                eleve.getPersonne().getNom(),
                eleve.getPersonne().getPrenom(),
                eleve.getPersonne().getEmailContact(),
                eleve.getPersonne().getTelephone(),
                eleve.getPersonne().getAdresse(),
                eleve.getMatricule(),
                eleve.getDateNaissance(),
                eleve.getPhotoUrl() == null ? null : "/eleves/" + eleve.getIdPersonne() + "/photo"
        );
    }

    public void updateEntity(EleveRequest request, Eleve eleve) {
        eleve.getPersonne().setNom(request.nom());
        eleve.getPersonne().setPrenom(request.prenom());
        eleve.getPersonne().setEmailContact(request.emailContact());
        eleve.getPersonne().setTelephone(request.telephone());
        eleve.getPersonne().setAdresse(request.adresse());
        eleve.setMatricule(request.matricule());
        eleve.setDateNaissance(request.dateNaissance());
    }
}
