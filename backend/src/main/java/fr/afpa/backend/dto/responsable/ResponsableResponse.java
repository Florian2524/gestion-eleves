package fr.afpa.backend.dto.responsable;

public record ResponsableResponse(
        Long idPersonne,
        String nom,
        String prenom,
        String emailContact,
        String telephone,
        String adresse,
        String profession
) {
}
