package fr.afpa.backend.dto.eleve;

import java.time.LocalDate;

public record EleveResponse(
        Long idPersonne,
        String nom,
        String prenom,
        String emailContact,
        String telephone,
        String adresse,
        String matricule,
        LocalDate dateNaissance,
        String photoUrl
) {
}