package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Responsable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponsableRepository
        extends JpaRepository<Responsable, Long> {
}