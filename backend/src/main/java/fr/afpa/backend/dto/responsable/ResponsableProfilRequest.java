package fr.afpa.backend.dto.responsable;

import jakarta.validation.constraints.*;


public record ResponsableProfilRequest(@Size(max = 150) String profession) {}
