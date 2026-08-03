package fr.afpa.backend.dto.authentication;

import fr.afpa.backend.entity.RoleUtilisateur;

public record AuthenticationResponse(
        String token,
        String tokenType,
        long expiresIn,
        Long idUtilisateur,
        Long idPersonne,
        String emailConnexion,
        RoleUtilisateur role
) {
}
