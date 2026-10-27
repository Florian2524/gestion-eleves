package fr.afpa.backend.integration;

import fr.afpa.backend.support.IsolatedPostgres;
import fr.afpa.backend.dto.classe.ClasseRequest;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurUpdateRequest;
import fr.afpa.backend.dto.responsable.ResponsableProfilRequest;
import fr.afpa.backend.entity.*;
import fr.afpa.backend.exception.*;
import fr.afpa.backend.repository.*;
import fr.afpa.backend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;

/** Transactions réellement distinctes ; toutes les fixtures sont dans PostgreSQL jetable. */
@SpringBootTest
class ProfilsConcurrenceIntegrationTest extends IsolatedPostgres {
    @Autowired PersonneRepository personnes;
    @Autowired CompteUtilisateurRepository comptes;
    @Autowired ProfilPersonneService profils;
    @Autowired EnseignantService enseignants;
    @Autowired ResponsableService responsables;
    @Autowired CompteUtilisateurService compteService;
    @Autowired ClasseService classes;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;

    private <T> T transaction(Supplier<T> action) {
        return new TransactionTemplate(manager).execute(status -> action.get());
    }
    private Long personne() {
        return personnes.saveAndFlush(new Personne("Concurrence", "Test", null, null, null)).getIdPersonne();
    }
    private void nettoyer(Long id) {
        // Uniquement les IDs créés par ce test, sur la connexion gardée par IsolatedPostgres.
        jdbc.update("DELETE FROM compte_utilisateur WHERE id_personne=?", id);
        jdbc.update("DELETE FROM responsable WHERE id_personne=?", id);
        jdbc.update("DELETE FROM enseignant WHERE id_personne=?", id);
        jdbc.update("DELETE FROM personne WHERE id_personne=?", id);
    }
    private List<String> ensemble(Runnable a, Runnable b) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = List.of(a,b).stream().map(action -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Départ concurrent absent");
                try { action.run(); return "OK"; }
                catch (DuplicateResourceException | ResourceInUseException | ResourceNotFoundException | InvalidAccountRoleException conflict) {
                    return conflict.getClass().getSimpleName();
                }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(futures.get(0).get(15, TimeUnit.SECONDS), futures.get(1).get(15, TimeUnit.SECONDS));
        }
    }

    @Test void deuxAjoutsNeCreentQuUnProfilSansEcrasement() throws Exception {
        Long id = transaction(this::personne);
        try {
            var result = ensemble(
                    () -> responsables.ajouterProfil(id, new ResponsableProfilRequest("A")),
                    () -> responsables.ajouterProfil(id, new ResponsableProfilRequest("B")));
            assertThat(result).containsExactlyInAnyOrder("OK", "DuplicateResourceException");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM responsable WHERE id_personne=?", Long.class, id)).isEqualTo(1);
            assertThat(responsables.findById(id).profession()).isIn("A", "B");
        } finally { nettoyer(id); }
    }

    @Test void affectationClasseEtRetraitNePerdentJamaisLaReferenceParSetNull() throws Exception {
        Long id = transaction(() -> {
            Long p = personne(); profils.ajouter(new Enseignant(personnes.findById(p).orElseThrow(), "CON-" + p)); return p;
        });
        Long cl = jdbc.queryForObject("INSERT INTO classe(nom,niveau,annee_scolaire) VALUES (?,'6','2026-2027') RETURNING id_classe", Long.class, "CON-" + id);
        try {
            var result = ensemble(() -> classes.update(cl, new ClasseRequest("CON-" + id,"6","2026-2027",id)), () -> enseignants.delete(id));
            assertThat(result).contains("OK");
            Long reference = jdbc.queryForObject("SELECT id_professeur_principal FROM classe WHERE id_classe=?", Long.class, cl);
            boolean existe = jdbc.queryForObject("SELECT count(*) FROM enseignant WHERE id_personne=?", Long.class, id) == 1;
            if (reference != null) {
                assertThat(reference).isEqualTo(id); assertThat(existe).isTrue();
                assertThat(result).contains("ResourceInUseException");
            } else {
                assertThat(existe).isFalse(); assertThat(result).contains("ResourceNotFoundException");
            }
        } finally { jdbc.update("DELETE FROM classe WHERE id_classe=?", cl); nettoyer(id); }
    }

    @Test void changementRoleEtRetraitGardentToujoursLeProfilRequis() throws Exception {
        Long id = transaction(() -> {
            Long p = personne(); var personne = personnes.findById(p).orElseThrow();
            profils.ajouter(new Enseignant(personne,"CON-" + p)); profils.ajouter(new Responsable(personne,null));
            comptes.saveAndFlush(new CompteUtilisateur(personne,"con-" + p + "@example.test","hash-conservé",RoleUtilisateur.ENSEIGNANT));
            return p;
        });
        Long compteId = jdbc.queryForObject("SELECT id_utilisateur FROM compte_utilisateur WHERE id_personne=?", Long.class, id);
        try {
            var result = ensemble(() -> compteService.update(compteId, new CompteUtilisateurUpdateRequest(
                    "con-" + id + "@example.test",null,RoleUtilisateur.RESPONSABLE,true)), () -> responsables.delete(id));
            assertThat(result).contains("OK");
            String role = jdbc.queryForObject("SELECT role FROM compte_utilisateur WHERE id_personne=?", String.class, id);
            boolean existe = jdbc.queryForObject("SELECT count(*) FROM responsable WHERE id_personne=?", Long.class, id) == 1;
            assertThat(existe).isEqualTo(role.equals("RESPONSABLE"));
            assertThat(jdbc.queryForObject("SELECT mot_de_passe_hash FROM compte_utilisateur WHERE id_personne=?", String.class, id)).isEqualTo("hash-conservé");
        } finally { nettoyer(id); }
    }

    @Test void echecDuProfilAnnuleAussiSaNouvelleIdentite() {
        Long id = transaction(() -> {
            Long p = personne(); profils.ajouter(new Enseignant(personnes.findById(p).orElseThrow(),"CON-" + p)); return p;
        });
        try {
            long avant = personnes.count();
            assertThatThrownBy(() -> transaction(() -> {
                var p = new Personne("Rollback", "Test", null,null,null);
                return profils.creer(p, new Enseignant(p,"CON-" + id));
            })).isInstanceOf(RuntimeException.class);
            assertThat(personnes.count()).isEqualTo(avant);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM personne WHERE nom='Rollback'",Long.class)).isZero();
        } finally { nettoyer(id); }
    }
    @Test void deuxDesactivationsConcurrentesConserventUnAdminActif() throws Exception {
        Long[] ids = transaction(() -> {
            Long a = personne(), b = personne();
            for (Long id : List.of(a,b)) comptes.saveAndFlush(new CompteUtilisateur(
                    personnes.findById(id).orElseThrow(), "admin-con-" + id + "@example.test", "hash", RoleUtilisateur.ADMIN));
            return new Long[]{a,b};
        });
        Long a = jdbc.queryForObject("SELECT id_utilisateur FROM compte_utilisateur WHERE id_personne=?", Long.class, ids[0]);
        Long b = jdbc.queryForObject("SELECT id_utilisateur FROM compte_utilisateur WHERE id_personne=?", Long.class, ids[1]);
        try {
            var result = ensemble(
                    () -> compteService.update(a,new CompteUtilisateurUpdateRequest("admin-con-" + ids[0] + "@example.test",null,RoleUtilisateur.ADMIN,false)),
                    () -> compteService.update(b,new CompteUtilisateurUpdateRequest("admin-con-" + ids[1] + "@example.test",null,RoleUtilisateur.ADMIN,false)));
            assertThat(result).containsExactlyInAnyOrder("OK","ResourceInUseException");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM compte_utilisateur WHERE id_personne IN (?,?) AND role='ADMIN' AND actif",Long.class,ids[0],ids[1])).isEqualTo(1);
        } finally { nettoyer(ids[0]); nettoyer(ids[1]); }
    }

}
