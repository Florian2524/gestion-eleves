package fr.afpa.backend.dto.enseignant;

public record EnseignantResponse(
        Long idPersonne,
        String nom,
        String prenom,
        String emailContact,
        String telephone,
        String adresse,
        String numeroEmploye
) {
}