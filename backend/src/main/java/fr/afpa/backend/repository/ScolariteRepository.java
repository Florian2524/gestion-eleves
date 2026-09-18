package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Scolarite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ScolariteRepository
        extends JpaRepository<Scolarite, Long> {
    @org.springframework.data.jpa.repository.Query("select count(s) > 0 from Scolarite s, ResponsabiliteLegale r where s.idScolarite = :idScolarite and r.eleve = s.eleve and r.responsable.idPersonne = :idResponsable")
    boolean estLieAuResponsable(Long idScolarite, Long idResponsable);

    @org.springframework.data.jpa.repository.Query("select count(s) > 0 from Scolarite s, Enseignement e where s.idScolarite = :idScolarite and e.classe = s.classe and e.enseignant.idPersonne = :idEnseignant")
    boolean concerneEnseignant(Long idScolarite, Long idEnseignant);

    boolean existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebut(
            Long idEleve,
            Long idClasse,
            LocalDate dateDebut
    );

    boolean existsByEleve_IdPersonneAndClasse_IdClasseAndDateDebutAndIdScolariteNot(
            Long idEleve,
            Long idClasse,
            LocalDate dateDebut,
            Long idScolarite
    );
}
