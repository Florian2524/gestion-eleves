package fr.afpa.backend.dto.classe;

public record ClasseResponse(
        Long idClasse,
        String nom,
        String niveau,
        String anneeScolaire,
        Long idProfesseurPrincipal
) {
}