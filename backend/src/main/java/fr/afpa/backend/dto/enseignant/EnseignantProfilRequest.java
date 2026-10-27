package fr.afpa.backend.dto.enseignant;

import jakarta.validation.constraints.*;


public record EnseignantProfilRequest(@NotBlank @Size(max = 50) String numeroEmploye) {}
