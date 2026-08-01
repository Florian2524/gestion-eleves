package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.scolarite.ScolariteRequest;
import fr.afpa.backend.dto.scolarite.ScolariteResponse;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.Scolarite;
import org.springframework.stereotype.Component;

@Component
public class ScolariteMapper {

    public Scolarite toEntity(
            ScolariteRequest request,
            Eleve eleve,
            Classe classe
    ) {
        return new Scolarite(
                eleve,
                classe,
                request.dateDebut(),
                request.dateFin(),
                request.statut()
        );
    }

    public ScolariteResponse toResponse(Scolarite scolarite) {
        return new ScolariteResponse(
                scolarite.getIdScolarite(),
                scolarite.getEleve().getIdPersonne(),
                scolarite.getClasse().getIdClasse(),
                scolarite.getDateDebut(),
                scolarite.getDateFin(),
                scolarite.getStatut()
        );
    }

    public void updateEntity(
            ScolariteRequest request,
            Eleve eleve,
            Classe classe,
            Scolarite scolarite
    ) {
        scolarite.setEleve(eleve);
        scolarite.setClasse(classe);
        scolarite.setDateDebut(request.dateDebut());
        scolarite.setDateFin(request.dateFin());
        scolarite.setStatut(request.statut());
    }
}