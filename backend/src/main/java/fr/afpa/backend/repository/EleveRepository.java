package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Eleve;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EleveRepository extends JpaRepository<Eleve, Long> {
    @org.springframework.data.jpa.repository.Query("select count(s) > 0 from Scolarite s, Enseignement e where s.eleve.idPersonne = :idEleve and e.classe = s.classe and e.enseignant.idPersonne = :idEnseignant")
    boolean concerneEnseignant(Long idEleve, Long idEnseignant);

    boolean existsByMatricule(String matricule);

    boolean existsByMatriculeAndIdPersonneNot(
            String matricule,
            Long idPersonne
    );
}
