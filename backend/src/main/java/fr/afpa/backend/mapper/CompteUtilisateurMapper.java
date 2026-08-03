package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurResponse;
import fr.afpa.backend.entity.CompteUtilisateur;
import fr.afpa.backend.entity.Personne;
import fr.afpa.backend.entity.RoleUtilisateur;
import org.springframework.stereotype.Component;

@Component
public class CompteUtilisateurMapper {

    public CompteUtilisateur toEntity(
            Personne personne,
            String emailConnexion,
            String motDePasseHash,
            RoleUtilisateur role
    ) {
        return new CompteUtilisateur(
                personne,
                emailConnexion,
                motDePasseHash,
                role
        );
    }

    public void updateEntity(
            CompteUtilisateur compteUtilisateur,
            String emailConnexion,
            RoleUtilisateur role,
            boolean actif,
            String nouveauMotDePasseHash
    ) {
        compteUtilisateur.setEmailConnexion(emailConnexion);
        compteUtilisateur.setRole(role);
        compteUtilisateur.setActif(actif);

        if (nouveauMotDePasseHash != null) {
            compteUtilisateur.setMotDePasseHash(
                    nouveauMotDePasseHash
            );
        }
    }

    public CompteUtilisateurResponse toResponse(
            CompteUtilisateur compteUtilisateur
    ) {
        Personne personne = compteUtilisateur.getPersonne();

        return new CompteUtilisateurResponse(
                compteUtilisateur.getIdUtilisateur(),
                personne.getIdPersonne(),
                personne.getNom(),
                personne.getPrenom(),
                compteUtilisateur.getEmailConnexion(),
                compteUtilisateur.getRole(),
                compteUtilisateur.isActif(),
                compteUtilisateur.getDateCreation()
        );
    }
}
