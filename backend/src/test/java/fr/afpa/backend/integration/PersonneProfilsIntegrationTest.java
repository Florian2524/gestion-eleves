package fr.afpa.backend.integration;

import fr.afpa.backend.support.IsolatedPostgres;
import fr.afpa.backend.dto.authentication.LoginRequest;
import fr.afpa.backend.dto.compteutilisateur.*;
import fr.afpa.backend.dto.eleve.*;
import fr.afpa.backend.dto.enseignant.*;
import fr.afpa.backend.dto.responsable.*;
import fr.afpa.backend.entity.*;
import fr.afpa.backend.exception.*;
import fr.afpa.backend.repository.*;
import fr.afpa.backend.security.*;
import fr.afpa.backend.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonneProfilsIntegrationTest extends IsolatedPostgres {
    @Autowired PersonneRepository personnes;
    @Autowired EleveRepository eleves;
    @Autowired EnseignantRepository enseignants;
    @Autowired ResponsableRepository responsables;
    @Autowired CompteUtilisateurRepository comptes;
    @Autowired ResponsabiliteLegaleRepository liens;
    @Autowired ProfilPersonneService profils;
    @Autowired EleveService eleveService;
    @Autowired EnseignantService enseignantService;
    @Autowired ResponsableService responsableService;
    @Autowired CompteUtilisateurService compteService;
    @Autowired AuthenticationService authentication;
    @Autowired FirstAdminBootstrap bootstrap;
    @Autowired BulletinCalculService calcul;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwtService;

    private Personne personne() {
        return personnes.saveAndFlush(new Personne("Identité", "Unique", "contact@example.test", "0102030405", "Adresse conservée"));
    }
    private void reload() { em.flush(); em.clear(); }
    private Enseignant enseignant(Personne p) { return profils.ajouter(new Enseignant(p, "ENS-" + p.getIdPersonne())); }
    private Eleve eleve(Personne p) { return profils.ajouter(new Eleve(p, "EL-" + p.getIdPersonne(), LocalDate.of(2012,1,1), null)); }
    private CompteUtilisateur compte(Personne p, RoleUtilisateur role) {
        return comptes.saveAndFlush(new CompteUtilisateur(p, "p" + p.getIdPersonne() + "@example.test", encoder.encode("Password123"), role));
    }
    private String token(CompteUtilisateur c) { return jwtService.generateToken(CompteUtilisateurPrincipal.fromEntity(c)); }
    private long scalar(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }

    @Test void personneSansProfilResteAutonome() {
        var p = personne(); Long id = p.getIdPersonne(); reload();
        assertThat(personnes.findById(id).orElseThrow().getNom()).isEqualTo("Identité");
        assertThat(eleves.existsById(id)).isFalse();
        assertThat(enseignants.existsById(id)).isFalse();
        assertThat(responsables.existsById(id)).isFalse();
        assertThat(profils.findAll()).filteredOn(x -> x.idPersonne().equals(id)).singleElement()
                .satisfies(x -> assertThat(x.profils()).isEmpty());
    }

    @ParameterizedTest @ValueSource(strings = {"ELEVE", "ENSEIGNANT", "RESPONSABLE"})
    void unProfilPartageLaCleEtSeSupprimeIndependamment(String type) {
        var p = personne(); Long id = p.getIdPersonne();
        switch (type) {
            case "ELEVE" -> eleve(p);
            case "ENSEIGNANT" -> enseignant(p);
            default -> profils.ajouter(new Responsable(p, "Profession"));
        }
        reload();
        switch (type) {
            case "ELEVE" -> { assertThat(eleves.findById(id).orElseThrow().getPersonne().getIdPersonne()).isEqualTo(id); eleveService.delete(id); }
            case "ENSEIGNANT" -> { assertThat(enseignants.findById(id).orElseThrow().getPersonne().getIdPersonne()).isEqualTo(id); enseignantService.delete(id); }
            default -> { assertThat(responsables.findById(id).orElseThrow().getPersonne().getIdPersonne()).isEqualTo(id); responsableService.delete(id); }
        }
        reload();
        assertThat(personnes.findById(id)).isPresent();
        assertThat(eleves.existsById(id)).isFalse();
        assertThat(enseignants.existsById(id)).isFalse();
        assertThat(responsables.existsById(id)).isFalse();
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void deuxProfilsUneSeuleIdentiteQuelQueSoitLOrdre(boolean enseignantPremier) {
        var p = personne(); Long id = p.getIdPersonne(); long avant = personnes.count();
        if (enseignantPremier) {
            enseignant(p);
            responsableService.ajouterProfil(id, new ResponsableProfilRequest("Architecte"));
        } else {
            profils.ajouter(new Responsable(p, "Architecte"));
            enseignantService.ajouterProfil(id, new EnseignantProfilRequest("ENS-" + id));
        }
        reload();
        var e = enseignants.findById(id).orElseThrow();
        var r = responsables.findById(id).orElseThrow();
        assertThat(e.getPersonne()).isSameAs(r.getPersonne());
        assertThat(e.getIdPersonne()).isEqualTo(r.getIdPersonne()).isEqualTo(id);
        assertThat(personnes.count()).isEqualTo(avant);
        assertThat(r.getProfession()).isEqualTo("Architecte");
        assertThat(profils.findAll()).filteredOn(x -> x.idPersonne().equals(id)).singleElement()
                .satisfies(x -> assertThat(x.profils()).containsExactly("ENSEIGNANT", "RESPONSABLE"));
    }

    @ParameterizedTest @ValueSource(strings = {"ELEVE", "ENSEIGNANT", "RESPONSABLE"})
    void refuseLeMemeProfilDeuxFois(String type) {
        var p = personne(); Long id = p.getIdPersonne();
        Runnable ajout = switch (type) {
            case "ELEVE" -> () -> eleveService.ajouterProfil(id, new EleveProfilRequest("EL-" + id, LocalDate.of(2012,1,1)));
            case "ENSEIGNANT" -> () -> enseignantService.ajouterProfil(id, new EnseignantProfilRequest("ENS-" + id));
            default -> () -> responsableService.ajouterProfil(id, new ResponsableProfilRequest("Initiale"));
        };
        ajout.run(); reload();
        assertThatThrownBy(ajout::run).isInstanceOf(DuplicateResourceException.class);
        assertThat(personnes.findById(id).orElseThrow().getAdresse()).isEqualTo("Adresse conservée");
    }

    @ParameterizedTest @ValueSource(strings = {"ENSEIGNANT", "RESPONSABLE"})
    void retireUnProfilEtConserveLAutreEtLeCompteAdmin(String retrait) {
        var p = personne(); Long id = p.getIdPersonne(); enseignant(p);
        responsableService.ajouterProfil(id, new ResponsableProfilRequest("Profession"));
        var c = compte(p, RoleUtilisateur.ADMIN); Long compteId = c.getIdUtilisateur(); String hash = c.getMotDePasseHash();
        reload();
        if (retrait.equals("ENSEIGNANT")) enseignantService.delete(id); else responsableService.delete(id);
        reload();
        assertThat(personnes.existsById(id)).isTrue();
        assertThat(enseignants.existsById(id)).isEqualTo(!retrait.equals("ENSEIGNANT"));
        assertThat(responsables.existsById(id)).isEqualTo(!retrait.equals("RESPONSABLE"));
        var conserve = comptes.findById(compteId).orElseThrow();
        assertThat(conserve.getMotDePasseHash()).isEqualTo(hash);
        assertThat(conserve.getRole()).isEqualTo(RoleUtilisateur.ADMIN);
        assertThat(conserve.isActif()).isTrue();
    }

    @Test void lesCoordonneesPartageesSePropagentSansChangerLesAttributsDeProfil() {
        var p = personne(); Long id = p.getIdPersonne(); enseignant(p);
        responsableService.ajouterProfil(id, new ResponsableProfilRequest("Profession"));
        enseignantService.update(id, new EnseignantRequest("Nom modifié", "Unique", "autre@example.test", null, "Adresse", "ENS-" + id));
        reload();
        assertThat(responsableService.findById(id).nom()).isEqualTo("Nom modifié");
        assertThat(responsableService.findById(id).profession()).isEqualTo("Profession");
        assertThat(enseignantService.findById(id).numeroEmploye()).isEqualTo("ENS-" + id);
    }

    @Test void lesPostHistoriquesCreentPersonneEtProfilSansRapprochementAutomatique() {
        var request = new EnseignantRequest("Même nom", "Même prénom", "partage@example.test", null, null, "NUM-A");
        var a = enseignantService.create(request);
        var b = responsableService.create(new ResponsableRequest(request.nom(), request.prenom(), request.emailContact(), null, null, "Profession"));
        reload();
        assertThat(a.idPersonne()).isNotEqualTo(b.idPersonne());
        assertThat(personnes.existsById(a.idPersonne())).isTrue();
        assertThat(enseignants.findById(a.idPersonne()).orElseThrow().getPersonne().getEmailContact()).isEqualTo(request.emailContact());
        var el = eleveService.create(new EleveRequest("Nom", "Prénom", null, null, null, "MAT-A", LocalDate.of(2012,1,1), null));
        reload();
        assertThat(eleves.findById(el.idPersonne()).orElseThrow().getPersonne().getPrenom()).isEqualTo("Prénom");
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void compteSpecialiseMemeInactifBloqueLeRetraitDuProfilRequis(boolean actif) {
        var p = personne(); Long id = p.getIdPersonne(); enseignant(p);
        var c = compte(p, RoleUtilisateur.ENSEIGNANT); c.setActif(actif); reload();
        assertThatThrownBy(() -> enseignantService.delete(id)).isInstanceOf(ResourceInUseException.class);
        assertThat(enseignants.existsById(id)).isTrue();
    }

    @Test void compteResponsableBloqueSonProfilMaisPasLAutre() {
        var p = personne(); Long id = p.getIdPersonne(); enseignant(p);
        responsableService.ajouterProfil(id, new ResponsableProfilRequest(null)); compte(p, RoleUtilisateur.RESPONSABLE);
        enseignantService.delete(id); reload();
        assertThatThrownBy(() -> responsableService.delete(id)).isInstanceOf(ResourceInUseException.class);
        assertThat(comptes.existsByPersonneIdPersonne(id)).isTrue();
        assertThat(personnes.existsById(id)).isTrue();
    }

    @Test void liensLegauxEtCleComposeeProtegentLesDeuxProfils() {
        var p = personne(); var enfant = personne(); Long id = p.getIdPersonne(), idEleve = enfant.getIdPersonne();
        var r = profils.ajouter(new Responsable(p, "Profession")); var el = eleve(enfant);
        liens.saveAndFlush(new ResponsabiliteLegale(r, el)); reload();
        var cle = new ResponsabiliteLegaleId(id, idEleve);
        assertThat(liens.findById(cle).orElseThrow().getResponsable().getPersonne().getNom()).isEqualTo("Identité");
        assertThatThrownBy(() -> responsableService.delete(id)).isInstanceOf(ResourceInUseException.class);
        assertThatThrownBy(() -> eleveService.delete(idEleve)).isInstanceOf(ResourceInUseException.class);
        assertThat(liens.existsById(cle)).isTrue();
    }

    private long classe(Long prof) {
        return jdbc.queryForObject("INSERT INTO classe(nom,niveau,annee_scolaire,id_professeur_principal) VALUES ('Test','6','2026-2027',?) RETURNING id_classe", Long.class, prof);
    }
    private long periode() {
        return jdbc.queryForObject("INSERT INTO periode(libelle,date_debut,date_fin,statut) VALUES ('T1','2026-09-01','2026-12-31','OUVERTE') RETURNING id_periode", Long.class);
    }
    private long scolarite(long eleve, long classe) {
        return jdbc.queryForObject("INSERT INTO scolarite(id_eleve,id_classe,date_debut,statut) VALUES (?,?,'2026-09-01','ACTIVE') RETURNING id_scolarite", Long.class, eleve, classe);
    }
    private long enseignement(long prof, long classe) {
        long matiere = jdbc.queryForObject("INSERT INTO matiere(code,nom) VALUES (?,'Maths') RETURNING id_matiere", Long.class, "MAT-" + prof);
        return jdbc.queryForObject("INSERT INTO enseignement(id_enseignant,id_classe,id_matiere,coefficient_matiere,date_debut) VALUES (?,?,?,2,'2026-09-01') RETURNING id_enseignement", Long.class, prof, classe, matiere);
    }

    @ParameterizedTest @ValueSource(strings = {"classe", "enseignement", "bulletin"})
    void protegeToutesLesReferencesEnseignantYComprisSetNull(String reference) {
        var p = personne(); Long id = p.getIdPersonne(); enseignant(p);
        long cl = classe(reference.equals("classe") ? id : null);
        if (reference.equals("enseignement")) {
            long ens = enseignement(id, cl);
            jdbc.update("UPDATE enseignement SET actif=false WHERE id_enseignement=?", ens);
        }
        if (reference.equals("bulletin")) {
            long el = eleve(personne()).getIdPersonne(); long sc = scolarite(el, cl); long per = periode();
            jdbc.update("INSERT INTO bulletin(id_scolarite,id_periode,id_validateur,statut) VALUES (?,?,?,'VALIDE')", sc, per, id);
        }
        reload();
        assertThatThrownBy(() -> enseignantService.delete(id)).isInstanceOf(ResourceInUseException.class);
        assertThat(enseignants.existsById(id)).isTrue();
        if (reference.equals("classe")) assertThat(scalar("SELECT count(*) FROM classe WHERE id_professeur_principal=?", id)).isEqualTo(1);
        if (reference.equals("bulletin")) assertThat(scalar("SELECT count(*) FROM bulletin WHERE id_validateur=?", id)).isEqualTo(1);
    }

    @ParameterizedTest @ValueSource(strings = {"inscription", "scolarite"})
    void protegeLesDonneesScolaires(String reference) {
        Long id = eleve(personne()).getIdPersonne();
        if (reference.equals("inscription")) jdbc.update("INSERT INTO inscription(id_eleve,date_inscription,statut) VALUES (?,'2026-09-01','ACTIVE')", id);
        else scolarite(id, classe(null));
        reload();
        assertThatThrownBy(() -> eleveService.delete(id)).isInstanceOf(ResourceInUseException.class);
        assertThat(eleves.existsById(id)).isTrue();
    }

    @Test void ajoutRetraitDuProfilNeChangeNiNotesNiPondererNiBulletinCalcule() {
        var p = personne(); Long id = p.getIdPersonne(); enseignant(p);
        long cl = classe(null), el = eleve(personne()).getIdPersonne(), sc = scolarite(el, cl), per = periode();
        long ens = enseignement(id, cl);
        long ev = jdbc.queryForObject("INSERT INTO evaluation(id_enseignement,id_periode,libelle,date_evaluation,type_evaluation,coefficient_evaluation,bareme) VALUES (?,?,'Test','2026-10-01','DEVOIR',3,25) RETURNING id_evaluation", Long.class, ens, per);
        jdbc.update("INSERT INTO note(id_scolarite,id_evaluation,valeur,commentaire,statut_note) VALUES (?,?,18,'Conserver','VALIDEE')", sc, ev);
        reload();
        var avant = calcul.calculer(sc, per);
        assertThat(avant.moyenneGenerale()).isEqualByComparingTo("14.40");
        responsableService.ajouterProfil(id, new ResponsableProfilRequest("Profession")); reload();
        assertThat(calcul.calculer(sc, per)).isEqualTo(avant);
        responsableService.delete(id); reload();
        assertThat(calcul.calculer(sc, per)).isEqualTo(avant);
        assertThat(scalar("SELECT count(*) FROM note WHERE id_scolarite=? AND valeur=18 AND commentaire='Conserver'", sc)).isEqualTo(1);
        assertThat(scalar("SELECT count(*) FROM ponderer WHERE id_classe=? AND coefficient_matiere=2", cl)).isEqualTo(1);
    }

    @Test void rattachementHttpConserveLIdentiteEtLeJsonHistorique() throws Exception {
        var p = personne(); Long id = p.getIdPersonne(); enseignant(p);
        mvc.perform(post("/personnes/{id}/responsable", id)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"profession\":\"Architecte\",\"nom\":\"Ne pas remplacer\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/responsables/" + id))
                .andExpect(jsonPath("$.idPersonne").value(id)).andExpect(jsonPath("$.nom").value("Identité"))
                .andExpect(jsonPath("$.profession").value("Architecte")).andExpect(jsonPath("$.personne").doesNotExist());
        mvc.perform(post("/personnes/{id}/responsable", id)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
        reload();
        assertThat(personnes.findById(id).orElseThrow().getTelephone()).isEqualTo("0102030405");
        assertThat(enseignants.findById(id).orElseThrow().getNumeroEmploye()).isEqualTo("ENS-" + id);
    }

    @Test void erreursHttpAbsenceValidationEtSuppressionProtegee() throws Exception {
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mvc.perform(post("/personnes/9223372036854775807/responsable").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        Long id = personne().getIdPersonne();
        mvc.perform(post("/personnes/{id}/eleve", id).with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        var p = personnes.findById(id).orElseThrow(); enseignant(p); compte(p, RoleUtilisateur.ENSEIGNANT);
        mvc.perform(delete("/enseignants/{id}", id).with(admin)).andExpect(status().isConflict());
        assertThat(enseignants.existsById(id)).isTrue();
    }

    @ParameterizedTest @ValueSource(strings = {"ENSEIGNANT", "RESPONSABLE"})
    void endpointsPersonnesExclusivementAdmin(String role) throws Exception {
        var auth = jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
        mvc.perform(get("/personnes").with(auth)).andExpect(status().isForbidden());
        mvc.perform(post("/personnes/1/responsable").with(auth)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test void pasDAccesAnonymeNiDeSuppressionDePersonneExposee() throws Exception {
        mvc.perform(get("/personnes")).andExpect(status().isUnauthorized());
        Long id = personne().getIdPersonne();
        mvc.perform(delete("/personnes/{id}", id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
        assertThat(personnes.existsById(id)).isTrue();
    }

    @Test void adminSansProfilLoginBootstrapEtDernierAdminPreserves() throws Exception {
        var p = personne(); var c = compte(p, RoleUtilisateur.ADMIN);
        Long id = p.getIdPersonne(), compteId = c.getIdUtilisateur(); String hash = c.getMotDePasseHash();
        bootstrap.run(null);
        var response = authentication.login(new LoginRequest(c.getEmailConnexion(), "Password123"));
        assertThat(response.idPersonne()).isEqualTo(id);
        assertThat(response.role()).isEqualTo(RoleUtilisateur.ADMIN);
        mvc.perform(get("/personnes").header("Authorization", "Bearer " + response.token()))
                .andExpect(status().isOk());
        responsableService.ajouterProfil(id, new ResponsableProfilRequest(null)); responsableService.delete(id); reload();
        assertThat(comptes.findById(compteId).orElseThrow().getMotDePasseHash()).isEqualTo(hash);
        assertThatThrownBy(() -> compteService.delete(compteId)).isInstanceOf(ResourceInUseException.class);
        assertThatThrownBy(() -> compteService.update(compteId, new CompteUtilisateurUpdateRequest(
                c.getEmailConnexion(), null, RoleUtilisateur.ADMIN, false))).isInstanceOf(ResourceInUseException.class);
        assertThat(comptes.findById(compteId).orElseThrow().isActif()).isTrue();
    }

    @Test void leDoubleProfilNAccordePasDeuxRolesEtLeJwtDevientObsoleteApresChangement() throws Exception {
        var p = personne(); Long id = p.getIdPersonne(); enseignant(p);
        var autreProf = personne(); enseignant(autreProf);
        long cl = classe(null); enseignement(autreProf.getIdPersonne(), cl);
        var enfant = personne(); var el = eleve(enfant); scolarite(enfant.getIdPersonne(), cl);
        var c = compte(p, RoleUtilisateur.ENSEIGNANT); Long compteId = c.getIdUtilisateur();
        String ancienToken = token(c);
        responsableService.ajouterProfil(id, new ResponsableProfilRequest(null));
        liens.saveAndFlush(new ResponsabiliteLegale(responsables.findById(id).orElseThrow(), el)); reload();
        mvc.perform(get("/eleves/{id}", enfant.getIdPersonne()).header("Authorization", "Bearer " + ancienToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/enseignements").header("Authorization", "Bearer " + ancienToken))
                .andExpect(status().isOk());
        compteService.update(compteId, new CompteUtilisateurUpdateRequest(c.getEmailConnexion(), null, RoleUtilisateur.RESPONSABLE, true));
        reload();
        mvc.perform(get("/eleves").header("Authorization", "Bearer " + ancienToken)).andExpect(status().isUnauthorized());
        var compte = comptes.findWithPersonneByIdUtilisateur(compteId).orElseThrow(); String responsableToken = token(compte);
        mvc.perform(get("/eleves/{id}", enfant.getIdPersonne()).header("Authorization", "Bearer " + responsableToken))
                .andExpect(status().isOk());
        mvc.perform(post("/notes").header("Authorization", "Bearer " + responsableToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        liens.deleteById(new ResponsabiliteLegaleId(id, enfant.getIdPersonne())); reload();
        mvc.perform(get("/eleves/{id}", enfant.getIdPersonne()).header("Authorization", "Bearer " + responsableToken))
                .andExpect(status().isForbidden());
    }

    @Test void roleRequisEtCompteUniqueSontControlesSurLesProfils() {
        var p = personne(); Long id = p.getIdPersonne();
        var request = new CompteUtilisateurCreateRequest(id, "unique@example.test", "Password123", RoleUtilisateur.ENSEIGNANT);
        assertThatThrownBy(() -> compteService.create(request)).isInstanceOf(InvalidAccountRoleException.class);
        enseignant(p); responsableService.ajouterProfil(id, new ResponsableProfilRequest(null));
        var c = compteService.create(request);
        assertThat(c.role()).isEqualTo(RoleUtilisateur.ENSEIGNANT);
        assertThatThrownBy(() -> compteService.create(new CompteUtilisateurCreateRequest(id, "autre@example.test", "Password123", RoleUtilisateur.RESPONSABLE)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test void schemaV1V2ValideSansNouvelleMigrationNiGenerateurDeProfil() {
        assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class)).containsExactly("1", "2");
        assertThat(jdbc.queryForList("SELECT checksum FROM flyway_schema_history WHERE success ORDER BY installed_rank", Integer.class))
                .containsExactly(-526686782, 1440610697);
        assertThat(scalar("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name IN ('eleve','enseignant','responsable') AND column_name='id_personne' AND is_identity='NO' AND column_default IS NULL")).isEqualTo(3);
        assertThat(scalar("SELECT count(*) FROM pg_constraint WHERE conname IN ('fk_eleve_personne','fk_enseignant_personne','fk_responsable_personne') AND contype='f' AND convalidated")).isEqualTo(3);
        assertThat(scalar("SELECT count(*) FROM pg_trigger WHERE tgname IN ('tr_enseignement_ponderer','tr_ponderer_cle_immuable') AND NOT tgisinternal")).isEqualTo(2);
    }
    @ParameterizedTest @ValueSource(strings = {"eleve", "enseignant"})
    void autresRattachementsHttpConserventLeMemeIdentifiant(String type) throws Exception {
        Long id = personne().getIdPersonne();
        String body = type.equals("eleve") ? "{\"matricule\":\"HTTP-EL\",\"dateNaissance\":\"2012-01-01\"}"
                : "{\"numeroEmploye\":\"HTTP-ENS\"}";
        mvc.perform(post("/personnes/{id}/{type}", id, type)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.idPersonne").value(id))
                .andExpect(jsonPath("$.nom").value("Identité"));
        reload();
        if (type.equals("eleve")) assertThat(eleves.findById(id).orElseThrow().getMatricule()).isEqualTo("HTTP-EL");
        else assertThat(enseignants.findById(id).orElseThrow().getNumeroEmploye()).isEqualTo("HTTP-ENS");
    }

}
