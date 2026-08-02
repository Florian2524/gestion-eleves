package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.enseignement.EnseignementRequest;
import fr.afpa.backend.dto.enseignement.EnseignementResponse;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Enseignant;
import fr.afpa.backend.entity.Enseignement;
import fr.afpa.backend.entity.Matiere;
import org.springframework.stereotype.Component;

@Component
public class EnseignementMapper {

    public Enseignement toEntity(
            EnseignementRequest request,
            Enseignant enseignant,
            Classe classe,
            Matiere matiere
    ) {
        Enseignement enseignement = new Enseignement(
                enseignant,
                classe,
                matiere,
                request.coefficientMatiere(),
                request.dateDebut(),
                request.dateFin()
        );

        enseignement.setActif(request.actif());

        return enseignement;
    }

    public EnseignementResponse toResponse(
            Enseignement enseignement
    ) {
        return new EnseignementResponse(
                enseignement.getIdEnseignement(),
                enseignement.getEnseignant().getIdPersonne(),
                enseignement.getClasse().getIdClasse(),
                enseignement.getMatiere().getIdMatiere(),
                enseignement.getCoefficientMatiere(),
                enseignement.getDateDebut(),
                enseignement.getDateFin(),
                enseignement.isActif()
        );
    }

    public void updateEntity(
            EnseignementRequest request,
            Enseignant enseignant,
            Classe classe,
            Matiere matiere,
            Enseignement enseignement
    ) {
        enseignement.setEnseignant(enseignant);
        enseignement.setClasse(classe);
        enseignement.setMatiere(matiere);
        enseignement.setCoefficientMatiere(
                request.coefficientMatiere()
        );
        enseignement.setDateDebut(request.dateDebut());
        enseignement.setDateFin(request.dateFin());
        enseignement.setActif(request.actif());
    }
}