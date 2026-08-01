package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Scolarite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ScolariteRepository
        extends JpaRepository<Scolarite, Long> {

    boolean existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebut(
            Long idEleve,
            Long idClasse,
            LocalDate dateDebut
    );

    boolean existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebutAndIdScolariteNot(
            Long idEleve,
            Long idClasse,
            LocalDate dateDebut,
            Long idScolarite
    );
}