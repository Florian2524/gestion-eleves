package fr.afpa.backend.dto.eleve;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record EleveProfilRequest(@NotBlank @Size(max = 50) String matricule,
        @NotNull @Past LocalDate dateNaissance) {}
