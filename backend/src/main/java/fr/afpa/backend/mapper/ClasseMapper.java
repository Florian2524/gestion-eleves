package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.classe.ClasseRequest;
import fr.afpa.backend.dto.classe.ClasseResponse;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Enseignant;
import org.springframework.stereotype.Component;

@Component
public class ClasseMapper {

    public Classe toEntity(
            ClasseRequest request,
            Enseignant professeurPrincipal
    ) {
        return new Classe(
                request.nom(),
                request.niveau(),
                request.anneeScolaire(),
                professeurPrincipal
        );
    }

    public ClasseResponse toResponse(Classe classe) {
        Long idProfesseurPrincipal =
                classe.getProfesseurPrincipal() == null
                        ? null
                        : classe.getProfesseurPrincipal().getIdPersonne();

        return new ClasseResponse(
                classe.getIdClasse(),
                classe.getNom(),
                classe.getNiveau(),
                classe.getAnneeScolaire(),
                idProfesseurPrincipal
        );
    }

    public void updateEntity(
            ClasseRequest request,
            Enseignant professeurPrincipal,
            Classe classe
    ) {
        classe.setNom(request.nom());
        classe.setNiveau(request.niveau());
        classe.setAnneeScolaire(request.anneeScolaire());
        classe.setProfesseurPrincipal(professeurPrincipal);
    }
}