package fr.afpa.backend.repository;

import fr.afpa.backend.entity.CompteUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
