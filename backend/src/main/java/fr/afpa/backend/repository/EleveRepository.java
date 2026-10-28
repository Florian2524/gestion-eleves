package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Eleve;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EleveRepository extends JpaRepository<Eleve, Long> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "personne")
    java.util.List<Eleve> findAll();

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "personne")
    java.util.Optional<Eleve> findById(Long id);

    @org.springframework.data.jpa.repository.Query("select p.idPersonne from Eleve p")
    java.util.List<Long> findProfileIds();

    @org.springframework.data.jpa.repository.Query("select count(s) > 0 from Scolarite s, Enseignement e where s.eleve.idPersonne = :idEleve and e.classe = s.classe and e.enseignant.idPersonne = :idEnseignant")
    boolean concerneEnseignant(Long idEleve, Long idEnseignant);

    boolean existsByMatricule(String matricule);

    boolean existsByPhotoUrl(String photoUrl);

    boolean existsByMatriculeAndIdPersonneNot(
            String matricule,
            Long idPersonne
    );
}
