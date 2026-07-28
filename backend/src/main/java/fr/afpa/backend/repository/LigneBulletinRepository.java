package fr.afpa.backend.repository;

import fr.afpa.backend.entity.LigneBulletin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LigneBulletinRepository
        extends JpaRepository<LigneBulletin, Long> {
}