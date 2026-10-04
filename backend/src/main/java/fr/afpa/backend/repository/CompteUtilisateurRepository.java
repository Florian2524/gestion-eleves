package fr.afpa.backend.repository;

import fr.afpa.backend.entity.CompteUtilisateur;
import fr.afpa.backend.entity.RoleUtilisateur;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface CompteUtilisateurRepository
        extends JpaRepository<CompteUtilisateur, Long> {

    boolean existsByEmailConnexion(String emailConnexion);

    boolean existsByEmailConnexionAndIdUtilisateurNot(
            String emailConnexion,
            Long idUtilisateur
    );

    boolean existsByPersonneIdPersonne(Long idPersonne);

    Optional<CompteUtilisateur> findByEmailConnexion(
            String emailConnexion
    );

    @EntityGraph(attributePaths = "personne")
    Optional<CompteUtilisateur> findWithPersonneByIdUtilisateur(
            Long idUtilisateur
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CompteUtilisateur> findAllByRoleAndActifTrue(
            RoleUtilisateur role
    );
}
