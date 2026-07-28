package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Enseignement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnseignementRepository
        extends JpaRepository<Enseignement, Long> {
}