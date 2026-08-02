package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Enseignement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface EnseignementRepository
        extends JpaRepository<Enseignement, Long> {

    boolean existsByEnseignant_IdPersonne(Long idPersonne);

    boolean existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebut(
            Long idEnseignant,
            Long idClasse,
            Long idMatiere,
            LocalDate dateDebut
    );

    boolean existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebutAndIdEnseignementNot(
            Long idEnseignant,
            Long idClasse,
            Long idMatiere,
            LocalDate dateDebut,
            Long idEnseignement
    );
}