package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.eleve.EleveRequest;
import fr.afpa.backend.dto.eleve.EleveResponse;
import fr.afpa.backend.entity.Eleve;
import org.springframework.stereotype.Component;

@Component
public class EleveMapper {

    public Eleve toEntity(EleveRequest request) {
        return new Eleve(
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.matricule(),
                request.dateNaissance(),
                null
        );
    }

    public EleveResponse toResponse(Eleve eleve) {
        return new EleveResponse(
                eleve.getIdPersonne(),
                eleve.getNom(),
                eleve.getPrenom(),
                eleve.getEmailContact(),
                eleve.getTelephone(),
                eleve.getAdresse(),
                eleve.getMatricule(),
                eleve.getDateNaissance(),
                eleve.getPhotoUrl() == null ? null : "/eleves/" + eleve.getIdPersonne() + "/photo"
        );
    }

    public void updateEntity(EleveRequest request, Eleve eleve) {
        eleve.setNom(request.nom());
        eleve.setPrenom(request.prenom());
        eleve.setEmailContact(request.emailContact());
        eleve.setTelephone(request.telephone());
        eleve.setAdresse(request.adresse());
        eleve.setMatricule(request.matricule());
        eleve.setDateNaissance(request.dateNaissance());
    }
}
