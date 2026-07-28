package fr.afpa.backend.dto.matiere;

public record MatiereResponse(
        Long idMatiere,
        String code,
        String nom
) {
}