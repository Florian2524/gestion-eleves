package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Classe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClasseRepository extends JpaRepository<Classe, Long> {

    boolean existsByNomAndAnneeScolaire(
            String nom,
            String anneeScolaire
    );

    boolean existsByNomAndAnneeScolaireAndIdClasseNot(
            String nom,
            String anneeScolaire,
            Long idClasse
    );
}