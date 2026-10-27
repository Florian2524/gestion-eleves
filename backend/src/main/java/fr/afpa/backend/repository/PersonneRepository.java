package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Personne;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PersonneRepository extends JpaRepository<Personne, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Personne p where p.idPersonne = :id")
    Optional<Personne> findForUpdate(@Param("id") Long id);
}
