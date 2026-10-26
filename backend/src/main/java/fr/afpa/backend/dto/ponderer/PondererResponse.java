package fr.afpa.backend.dto.ponderer;

import java.math.BigDecimal;

public record PondererResponse(Long idClasse, Long idMatiere, BigDecimal coefficientMatiere) { }
