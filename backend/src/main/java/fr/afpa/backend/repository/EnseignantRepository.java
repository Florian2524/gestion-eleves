package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Enseignant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnseignantRepository
        extends JpaRepository<Enseignant, Long> {

    boolean existsByNumeroEmploye(String numeroEmploye);

    boolean existsByNumeroEmployeAndIdPersonneNot(
            String numeroEmploye,
            Long idPersonne
    );
}