package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Periode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodeRepository
        extends JpaRepository<Periode, Long> {
}