package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Matiere;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatiereRepository
        extends JpaRepository<Matiere, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdMatiereNot(
            String code,
            Long idMatiere
    );
}