package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Enseignement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EnseignementRepository
        extends JpaRepository<Enseignement, Long> {
    @org.springframework.data.jpa.repository.Query("select count(e) > 0 from Enseignement e, Scolarite s, ResponsabiliteLegale r where e.idEnseignement = :idEnseignement and s.classe = e.classe and r.eleve = s.eleve and r.responsable.idPersonne = :idResponsable")
    boolean concerneResponsable(Long idEnseignement, Long idResponsable);

    List<Enseignement> findAllByClasse_IdClasse(
            Long idClasse
    );

    List<Enseignement> findAllByEnseignant_IdPersonne(
            Long idPersonne
    );

    boolean existsByIdEnseignementAndEnseignant_IdPersonne(
            Long idEnseignement,
            Long idPersonne
    );

    boolean existsByEnseignant_IdPersonne(
            Long idPersonne
    );

    boolean existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebut(
            Long idEnseignant,
            Long idClasse,
            Long idMatiere,
            LocalDate dateDebut
    );

    boolean existsByEnseignant_IdPersonneAndClasse_IdClasseAndMatiere_IdMatiereAndDateDebutAndIdEnseignementNot(
            Long idEnseignant,
            Long idClasse,
            Long idMatiere,
            LocalDate dateDebut,
            Long idEnseignement
    );
}
