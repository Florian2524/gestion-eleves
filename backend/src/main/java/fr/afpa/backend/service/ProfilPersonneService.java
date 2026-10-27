package fr.afpa.backend.service;

import fr.afpa.backend.dto.personne.PersonneResponse;
import fr.afpa.backend.entity.*;
import fr.afpa.backend.exception.ResourceInUseException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Verrou commun aux profils, comptes et affectations : personne avant toute mutation. */
@Service
@Transactional
public class ProfilPersonneService {
    private final PersonneRepository personnes;
    private final EleveRepository eleves;
    private final EnseignantRepository enseignants;
    private final ResponsableRepository responsables;
    private final CompteUtilisateurRepository comptes;
    private final InscriptionRepository inscriptions;
    private final ScolariteRepository scolarites;
    private final ResponsabiliteLegaleRepository liens;
    private final ClasseRepository classes;
    private final EnseignementRepository enseignements;
    private final BulletinRepository bulletins;
    private final EntityManager em;

    public ProfilPersonneService(PersonneRepository personnes, EleveRepository eleves,
            EnseignantRepository enseignants, ResponsableRepository responsables,
            CompteUtilisateurRepository comptes, InscriptionRepository inscriptions,
            ScolariteRepository scolarites, ResponsabiliteLegaleRepository liens,
            ClasseRepository classes, EnseignementRepository enseignements,
            BulletinRepository bulletins, EntityManager em) {
        this.personnes = personnes; this.eleves = eleves; this.enseignants = enseignants;
        this.responsables = responsables; this.comptes = comptes; this.inscriptions = inscriptions;
        this.scolarites = scolarites; this.liens = liens; this.classes = classes;
        this.enseignements = enseignements; this.bulletins = bulletins; this.em = em;
    }

    public Personne verrouiller(Long id) {
        return personnes.findForUpdate(id).orElseThrow(() -> new ResourceNotFoundException(
                "La personne ayant l'identifiant " + id + " est introuvable."));
    }

    public void verrouillerTous(Long... ids) {
        Arrays.stream(ids).filter(Objects::nonNull).distinct().sorted().forEach(this::verrouiller);
    }

    public void verrouillerCompte(CompteUtilisateur compte) {
        verrouiller(compte.getPersonne().getIdPersonne());
        // Un autre changement du même compte a pu terminer pendant l'attente du verrou.
        em.refresh(compte);
    }

    public <T> T creer(Personne personne, T profil) {
        em.persist(personne);
        return ajouter(profil);
    }

    public <T> T ajouter(T profil) {
        // persist explicite : un rattachement ne doit jamais fusionner/écraser un profil.
        em.persist(profil);
        em.flush();
        return profil;
    }

    public void verifierSuppressionEleve(Long id) {
        verrouiller(id);
        if (inscriptions.existsByEleve_IdPersonne(id) || scolarites.existsByEleve_IdPersonne(id)
                || liens.existsByEleve_IdPersonne(id)) {
            throw new ResourceInUseException("Impossible de retirer le profil élève : des données scolaires ou responsabilités légales le référencent.");
        }
    }

    public void verifierSuppressionEnseignant(Long id) {
        verrouiller(id);
        if (comptes.existsByPersonne_IdPersonneAndRole(id, RoleUtilisateur.ENSEIGNANT)
                || enseignements.existsByEnseignant_IdPersonne(id)
                || classes.existsByProfesseurPrincipal_IdPersonne(id)
                || bulletins.existsByValidateur_IdPersonne(id)) {
            throw new ResourceInUseException("Impossible de retirer le profil enseignant : un compte, enseignement, classe ou bulletin le référence.");
        }
    }

    public void verifierSuppressionResponsable(Long id) {
        verrouiller(id);
        if (comptes.existsByPersonne_IdPersonneAndRole(id, RoleUtilisateur.RESPONSABLE)
                || liens.existsByResponsable_IdPersonne(id)) {
            throw new ResourceInUseException("Impossible de retirer le profil responsable : un compte ou une responsabilité légale le référence.");
        }
    }

    @Transactional(readOnly = true)
    public List<PersonneResponse> findAll() {
        Set<Long> idsEleves = new HashSet<>(eleves.findProfileIds());
        Set<Long> idsEnseignants = new HashSet<>(enseignants.findProfileIds());
        Set<Long> idsResponsables = new HashSet<>(responsables.findProfileIds());
        return personnes.findAll(Sort.by("nom", "prenom", "idPersonne")).stream().map(p -> {
            List<String> profils = new ArrayList<>();
            if (idsEleves.contains(p.getIdPersonne())) profils.add("ELEVE");
            if (idsEnseignants.contains(p.getIdPersonne())) profils.add("ENSEIGNANT");
            if (idsResponsables.contains(p.getIdPersonne())) profils.add("RESPONSABLE");
            return new PersonneResponse(p.getIdPersonne(), p.getNom(), p.getPrenom(),
                    p.getEmailContact(), List.copyOf(profils));
        }).toList();
    }
}
