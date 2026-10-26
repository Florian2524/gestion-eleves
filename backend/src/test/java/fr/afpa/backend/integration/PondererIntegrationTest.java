package fr.afpa.backend.integration;

import fr.afpa.backend.support.IsolatedPostgres;
import fr.afpa.backend.dto.enseignement.EnseignementRequest;
import fr.afpa.backend.dto.ponderer.PondererRequest;
import fr.afpa.backend.entity.*;
import fr.afpa.backend.exception.*;
import fr.afpa.backend.repository.*;
import fr.afpa.backend.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PondererIntegrationTest extends IsolatedPostgres {
    @Autowired JdbcTemplate jdbc;
    @Autowired PondererService ponderations;
    @Autowired EnseignementService enseignements;
    @Autowired ClasseService classes;
    @Autowired MatiereService matieres;
    @Autowired PondererRepository pondererRepository;
    @Autowired ClasseRepository classeRepository;
    @Autowired MatiereRepository matiereRepository;
    @Autowired EnseignementRepository enseignementRepository;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    private Long teacher, classe, autreClasse, matiere, enseignement;

    @BeforeEach void seed() {
        teacher = jdbc.queryForObject("INSERT INTO personne(nom,prenom) VALUES ('Test','Prof') RETURNING id_personne", Long.class);
        jdbc.update("INSERT INTO enseignant(id_personne,numero_employe) VALUES (?,?)", teacher, "TEST-" + teacher);
        classe = jdbc.queryForObject("INSERT INTO classe(nom,niveau,annee_scolaire) VALUES ('Test','6','2026-2027') RETURNING id_classe", Long.class);
        autreClasse = jdbc.queryForObject("INSERT INTO classe(nom,niveau,annee_scolaire) VALUES ('Autre','6','2026-2027') RETURNING id_classe", Long.class);
        matiere = jdbc.queryForObject("INSERT INTO matiere(code,nom) VALUES ('TEST','Maths') RETURNING id_matiere", Long.class);
        enseignement = jdbc.queryForObject("INSERT INTO enseignement(id_enseignant,id_classe,id_matiere,coefficient_matiere,date_debut) VALUES (?,?,?,2,'2026-09-01') RETURNING id_enseignement",
                Long.class, teacher, classe, matiere);
    }
    private EnseignementRequest teaching(Long idClasse, String coefficient, String debut) {
        return new EnseignementRequest(teacher, idClasse, matiere, new BigDecimal(coefficient), LocalDate.parse(debut), null, true);
    }
    private PondererRequest weighting(Long idClasse, String coefficient) {
        return new PondererRequest(idClasse, matiere, new BigDecimal(coefficient));
    }
    @Test void mapsCompositeKeyAndReloadsWithoutCascadingToParents() {
        var entity = new Ponderer(classeRepository.getReferenceById(autreClasse), matiereRepository.getReferenceById(matiere), new BigDecimal("3.25"));
        pondererRepository.saveAndFlush(entity);
        em.clear();
        var found = pondererRepository.findById(new PondererId(autreClasse, matiere)).orElseThrow();
        assertThat(found.getId()).isEqualTo(new PondererId(autreClasse, matiere));
        assertThat(found.getClasse().getNom()).isEqualTo("Autre");
        assertThat(found.getMatiere().getNom()).isEqualTo("Maths");
        assertThat(found.getCoefficientMatiere()).isEqualByComparingTo("3.25");
        pondererRepository.delete(found);
        pondererRepository.flush();
        assertThat(classeRepository.existsById(autreClasse)).isTrue();
        assertThat(matiereRepository.existsById(matiere)).isTrue();
    }
    @Test void teachingCreationInitializesNewPairAndRetainsExistingCoefficient() {
        var created = enseignements.create(teaching(autreClasse, "3.25", "2026-09-01"));
        assertThat(created.coefficientMatiere()).isEqualByComparingTo("3.25");
        assertThat(ponderations.findById(autreClasse, matiere).coefficientMatiere()).isEqualByComparingTo("3.25");
        var second = enseignements.create(teaching(classe, "2.00", "2026-10-01"));
        assertThat(second.coefficientMatiere()).isEqualByComparingTo("2");
    }
    @Test void rejectsDivergentTeachingCreation() {
        assertThatThrownBy(() -> enseignements.create(teaching(classe, "7", "2026-10-01")))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(enseignementRepository.findAllByClasse_IdClasse(classe)).hasSize(1);
    }
    @Test void rejectsDivergentTeachingUpdateBeforeChangingOtherFields() {
        assertThatThrownBy(() -> enseignements.update(enseignement, teaching(classe, "7", "2026-10-01")))
                .isInstanceOf(DuplicateResourceException.class);
        em.clear();
        assertThat(enseignementRepository.findById(enseignement).orElseThrow().getDateDebut()).isEqualTo(LocalDate.of(2026,9,1));
    }
    @Test void changingTeachingPairInitializesTargetAndPreservesSource() {
        enseignements.update(enseignement, teaching(autreClasse, "3", "2026-09-01"));
        em.clear();
        assertThat(ponderations.findById(classe, matiere).coefficientMatiere()).isEqualByComparingTo("2");
        assertThat(ponderations.findById(autreClasse, matiere).coefficientMatiere()).isEqualByComparingTo("3");
        assertThat(enseignementRepository.findById(enseignement).orElseThrow().getClasse().getIdClasse()).isEqualTo(autreClasse);
    }
    @Test void pondererUpdateCascadesAndClearsStaleJpaMirror() {
        enseignements.create(teaching(classe, "2", "2026-10-01"));
        enseignementRepository.findAllByClasse_IdClasse(classe); // charger les anciens miroirs
        ponderations.update(classe, matiere, weighting(classe, "4.50"));
        assertThat(enseignementRepository.findAllByClasse_IdClasse(classe)).hasSize(2)
                .allSatisfy(e -> assertThat(e.getCoefficientMatiere()).isEqualByComparingTo("4.50"));
        assertThat(enseignements.findById(enseignement).coefficientMatiere()).isEqualByComparingTo("4.50");
    }
    @Test void protectsReferencedPondererAndParents() {
        assertThatThrownBy(() -> ponderations.delete(classe, matiere)).isInstanceOf(ResourceInUseException.class);
        assertThatThrownBy(() -> classes.delete(classe)).isInstanceOf(ResourceInUseException.class);
        assertThatThrownBy(() -> matieres.delete(matiere)).isInstanceOf(ResourceInUseException.class);
    }
    @Test void coefficientAdministrationIsRestrictedToAdmin() throws Exception {
        mvc.perform(get("/ponderations")).andExpect(status().isUnauthorized());
        for (String role : new String[]{"ENSEIGNANT", "RESPONSABLE"}) {
            var token = jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
            mvc.perform(get("/ponderations").with(token)).andExpect(status().isForbidden());
            mvc.perform(post("/ponderations").with(token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/ponderations/1/1").with(token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/ponderations/1/1").with(token)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/ponderations").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
    @Test void adminCrudValidatesDtoAndReturnsCompositeLocation() throws Exception {
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        String body = "{\"idClasse\":" + autreClasse + ",\"idMatiere\":" + matiere + ",\"coefficientMatiere\":3}";
        String path = "/ponderations/" + autreClasse + "/" + matiere;
        mvc.perform(post("/ponderations").with(admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().string("Location", path));
        mvc.perform(put(path).with(admin).contentType(MediaType.APPLICATION_JSON).content(body.replace(":3}", ":4.25}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.coefficientMatiere").value(4.25));
        mvc.perform(get(path).with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.coefficientMatiere").value(4.25));
        for (String value : new String[]{"0", "-1", "null", "1.001", "1000"}) {
            mvc.perform(put(path).with(admin).contentType(MediaType.APPLICATION_JSON).content(body.replace(":3}", ":" + value + "}")))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(delete(path).with(admin)).andExpect(status().isNoContent());
        mvc.perform(get(path).with(admin)).andExpect(status().isNotFound());
    }
    @Test void apiRejectsDuplicatePair() throws Exception {
        mvc.perform(post("/ponderations").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"idClasse\":" + classe + ",\"idMatiere\":" + matiere + ",\"coefficientMatiere\":2}"))
                .andExpect(status().isConflict());
    }
}
