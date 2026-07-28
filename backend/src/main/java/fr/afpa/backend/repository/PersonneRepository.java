package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Personne;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonneRepository extends JpaRepository<Personne, Long> {
}