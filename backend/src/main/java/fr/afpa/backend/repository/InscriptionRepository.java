package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {
}