package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Responsable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponsableRepository
        extends JpaRepository<Responsable, Long> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "personne")
    java.util.List<Responsable> findAll();

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "personne")
    java.util.Optional<Responsable> findById(Long id);

    @org.springframework.data.jpa.repository.Query("select p.idPersonne from Responsable p")
    java.util.List<Long> findProfileIds();

}