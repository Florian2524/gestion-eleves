package fr.afpa.backend.dto.responsabilitelegale;

public record ResponsabiliteLegaleResponse(
        Long idResponsable,
        String nomResponsable,
        String prenomResponsable,
        Long idEleve,
        String nomEleve,
        String prenomEleve
) {
}
