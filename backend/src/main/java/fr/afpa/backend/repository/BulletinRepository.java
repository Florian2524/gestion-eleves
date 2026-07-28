package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Bulletin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BulletinRepository
        extends JpaRepository<Bulletin, Long> {
}