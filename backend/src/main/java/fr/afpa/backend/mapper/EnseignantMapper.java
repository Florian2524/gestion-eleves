package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.enseignant.EnseignantRequest;
import fr.afpa.backend.dto.enseignant.EnseignantResponse;
import fr.afpa.backend.entity.Enseignant;
import fr.afpa.backend.entity.Personne;
import org.springframework.stereotype.Component;

@Component
public class EnseignantMapper {

    public Enseignant toEntity(EnseignantRequest request) {
        return new Enseignant(
                new Personne(request.nom(), request.prenom(), request.emailContact(),
                        request.telephone(), request.adresse()),
                request.numeroEmploye()
        );
    }

    public EnseignantResponse toResponse(Enseignant enseignant) {
        return new EnseignantResponse(
                enseignant.getIdPersonne(),
                enseignant.getPersonne().getNom(),
                enseignant.getPersonne().getPrenom(),
                enseignant.getPersonne().getEmailContact(),
                enseignant.getPersonne().getTelephone(),
                enseignant.getPersonne().getAdresse(),
                enseignant.getNumeroEmploye()
        );
    }

    public void updateEntity(
            EnseignantRequest request,
            Enseignant enseignant
    ) {
        enseignant.getPersonne().setNom(request.nom());
        enseignant.getPersonne().setPrenom(request.prenom());
        enseignant.getPersonne().setEmailContact(request.emailContact());
        enseignant.getPersonne().setTelephone(request.telephone());
        enseignant.getPersonne().setAdresse(request.adresse());
        enseignant.setNumeroEmploye(request.numeroEmploye());
    }
}
