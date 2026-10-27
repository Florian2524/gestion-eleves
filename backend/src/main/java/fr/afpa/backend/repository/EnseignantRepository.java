package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Enseignant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnseignantRepository
        extends JpaRepository<Enseignant, Long> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "personne")
    java.util.List<Enseignant> findAll();

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "personne")
    java.util.Optional<Enseignant> findById(Long id);

    @org.springframework.data.jpa.repository.Query("select p.idPersonne from Enseignant p")
    java.util.List<Long> findProfileIds();


    boolean existsByNumeroEmploye(String numeroEmploye);

    boolean existsByNumeroEmployeAndIdPersonneNot(
            String numeroEmploye,
            Long idPersonne
    );
}