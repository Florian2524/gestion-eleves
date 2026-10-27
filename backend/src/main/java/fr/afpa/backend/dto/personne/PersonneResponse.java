package fr.afpa.backend.dto.personne;

import java.util.List;

public record PersonneResponse(Long idPersonne, String nom, String prenom,
                               String emailContact, List<String> profils) {}
