package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Eleve;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EleveRepository extends JpaRepository<Eleve, Long> {

    boolean existsByMatricule(String matricule);

    boolean existsByMatriculeAndIdPersonneNot(
            String matricule,
            Long idPersonne
    );
}