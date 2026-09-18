package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Classe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClasseRepository extends JpaRepository<Classe, Long> {
    @org.springframework.data.jpa.repository.Query("select count(e) > 0 from Enseignement e where e.classe.idClasse = :idClasse and e.enseignant.idPersonne = :idEnseignant")
    boolean concerneEnseignant(Long idClasse, Long idEnseignant);

    @org.springframework.data.jpa.repository.Query("select count(s) > 0 from Scolarite s, ResponsabiliteLegale r where s.classe.idClasse = :idClasse and r.eleve = s.eleve and r.responsable.idPersonne = :idResponsable")
    boolean concerneResponsable(Long idClasse, Long idResponsable);

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
