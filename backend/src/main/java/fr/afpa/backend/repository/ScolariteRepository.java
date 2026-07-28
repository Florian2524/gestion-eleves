package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Scolarite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScolariteRepository extends JpaRepository<Scolarite, Long> {
}