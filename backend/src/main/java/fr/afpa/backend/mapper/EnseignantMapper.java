package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.enseignant.EnseignantRequest;
import fr.afpa.backend.dto.enseignant.EnseignantResponse;
import fr.afpa.backend.entity.Enseignant;
import org.springframework.stereotype.Component;

@Component
public class EnseignantMapper {

    public Enseignant toEntity(EnseignantRequest request) {
        return new Enseignant(
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.numeroEmploye()
        );
    }

    public EnseignantResponse toResponse(Enseignant enseignant) {
        return new EnseignantResponse(
                enseignant.getIdPersonne(),
                enseignant.getNom(),
                enseignant.getPrenom(),
                enseignant.getEmailContact(),
                enseignant.getTelephone(),
                enseignant.getAdresse(),
                enseignant.getNumeroEmploye()
        );
    }

    public void updateEntity(
            EnseignantRequest request,
            Enseignant enseignant
    ) {
        enseignant.setNom(request.nom());
        enseignant.setPrenom(request.prenom());
        enseignant.setEmailContact(request.emailContact());
        enseignant.setTelephone(request.telephone());
        enseignant.setAdresse(request.adresse());
        enseignant.setNumeroEmploye(request.numeroEmploye());
    }
}