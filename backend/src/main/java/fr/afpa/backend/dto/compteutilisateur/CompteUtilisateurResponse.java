package fr.afpa.backend.dto.compteutilisateur;

import fr.afpa.backend.entity.RoleUtilisateur;

import java.time.OffsetDateTime;

public record CompteUtilisateurResponse(
        Long idUtilisateur,
        Long idPersonne,
        String nom,
        String prenom,
        String emailConnexion,
        RoleUtilisateur role,
        boolean actif,
        OffsetDateTime dateCreation
) {
}
