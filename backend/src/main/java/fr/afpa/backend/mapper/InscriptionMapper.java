package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.inscription.InscriptionRequest;
import fr.afpa.backend.dto.inscription.InscriptionResponse;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.Inscription;
import org.springframework.stereotype.Component;

@Component
public class InscriptionMapper {

    public Inscription toEntity(
            InscriptionRequest request,
            Eleve eleve
    ) {
        return new Inscription(
                eleve,
                request.dateInscription(),
                request.dateFin(),
                request.statut()
        );
    }

    public InscriptionResponse toResponse(
            Inscription inscription
    ) {
        return new InscriptionResponse(
                inscription.getIdInscription(),
                inscription.getEleve().getIdPersonne(),
                inscription.getDateInscription(),
                inscription.getDateFin(),
                inscription.getStatut()
        );
    }

    public void updateEntity(
            InscriptionRequest request,
            Eleve eleve,
            Inscription inscription
    ) {
        inscription.setEleve(eleve);
        inscription.setDateInscription(
                request.dateInscription()
        );
        inscription.setDateFin(request.dateFin());
        inscription.setStatut(request.statut());
    }
}