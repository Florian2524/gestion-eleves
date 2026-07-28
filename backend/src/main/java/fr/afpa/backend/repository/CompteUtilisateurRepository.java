package fr.afpa.backend.repository;

import fr.afpa.backend.entity.CompteUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompteUtilisateurRepository
        extends JpaRepository<CompteUtilisateur, Long> {
}