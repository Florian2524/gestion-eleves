package fr.afpa.backend.integration;

import fr.afpa.backend.dto.periode.PeriodeRequest;
import fr.afpa.backend.exception.BusinessRuleViolationException;
import fr.afpa.backend.repository.PeriodeRepository;
import fr.afpa.backend.service.PeriodeService;
import fr.afpa.backend.support.IsolatedPostgres;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Transactions terminées puis nouvelles lectures ; aucune datasource implicite de développement. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PeriodeAnneeScolaireIntegrationTest extends IsolatedPostgres {
    @Autowired PeriodeService service;
    @Autowired PeriodeRepository repository;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;
    private final List<Long> ids = new ArrayList<>();

    @AfterEach void cleanup() {
        new TransactionTemplate(manager).executeWithoutResult(status -> repository.deleteAllById(ids));
    }

    private PeriodeRequest request(String year) {
        return new PeriodeRequest("Test année", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31), null, null, "OUVERTE", year);
    }

    private long create(String year) {
        long id = service.create(request(year)).idPeriode();
        ids.add(id);
        return id;
    }

    private void assertReloaded(long id, String year) {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            em.flush();
            em.clear();
            assertThat(repository.findById(id).orElseThrow().getAnneeScolaire()).isEqualTo(year);
        });
        assertThat(service.findById(id).anneeScolaire()).isEqualTo(year);
    }

    @Test void commitsYearThenUpdatesAndClearsItWithoutChangingOtherFields() {
        long id = create("2026-2027");
        assertReloaded(id, "2026-2027");
        service.update(id, request("2027-2028"));
        assertReloaded(id, "2027-2028");
        service.update(id, request(null));
        assertReloaded(id, null);
        var p = service.findById(id);
        assertThat(p.dateDebut()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(p.statut()).isEqualTo("OUVERTE");
    }

    @Test void nullYearRemainsReadableInListAndSingleResponse() throws Exception {
        long id = create(null);
        assertReloaded(id, null);
        assertThat(service.findAll()).anySatisfy(p -> {
            assertThat(p.idPeriode()).isEqualTo(id);
            assertThat(p.anneeScolaire()).isNull();
        });
        mvc.perform(get("/periodes/{id}", id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("\"anneeScolaire\":null")));
    }

    @Test void proxiedServiceRejectsInvalidYearBeforeAnyWrite() {
        long id = create("2026-2027");
        assertThatThrownBy(() -> service.update(id, request("2026-2028")))
                .isInstanceOf(BusinessRuleViolationException.class).hasMessageContaining("consécutives");
        assertReloaded(id, "2026-2027");
        assertThatThrownBy(() -> service.create(request("26-27")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test void httpCreateUpdateAndExplicitNullUseExistingRoutes() throws Exception {
        String body = """
                {"libelle":"API année","dateDebut":"2026-09-01","dateFin":"2026-12-31",
                 "statut":"OUVERTE","anneeScolaire":"2026-2027"}
                """;
        var response = mvc.perform(post("/periodes").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.anneeScolaire").value("2026-2027")).andReturn();
        long id = Long.parseLong(response.getResponse().getHeader("Location").replace("/periodes/", ""));
        ids.add(id);
        mvc.perform(put("/periodes/{id}", id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("2026-2027", "2027-2028")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.anneeScolaire").value("2027-2028"));
        assertReloaded(id, "2027-2028");
        mvc.perform(put("/periodes/{id}", id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("\"2026-2027\"", "null")))
                .andExpect(status().isOk());
        assertReloaded(id, null);
    }

    @Test void legacyJsonWithoutYearRemainsAccepted() throws Exception {
        var response = mvc.perform(post("/periodes").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"libelle":"Ancien client","dateDebut":"2026-09-01","dateFin":"2026-12-31","statut":"OUVERTE"}
                        """))
                .andExpect(status().isCreated()).andReturn();
        long id = Long.parseLong(response.getResponse().getHeader("Location").replace("/periodes/", ""));
        ids.add(id);
        assertReloaded(id, null);
    }

    @Test void invalidYearsReturn400OnBothCreateAndUpdate() throws Exception {
        long id = create(null);
        for (String year : List.of("", "2026/2027", "2026-2028", "2026-2026", "2026-2027 ", "２０２６-２０２７", "9999-0000")) {
            String body = """
                    {"libelle":"Invalide","dateDebut":"2026-09-01","dateFin":"2026-12-31","statut":"OUVERTE","anneeScolaire":"%s"}
                    """.formatted(year);
            for (var builder : List.of(post("/periodes"), put("/periodes/{id}", id))) {
                mvc.perform(builder.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.validationErrors.anneeScolaireValide").value(PeriodeRequest.ANNEE_SCOLAIRE_MESSAGE));
            }
        }
        assertReloaded(id, null);
    }

    @Test void legacyPutWithoutYearPreservesExistingYearWhileUpdatingOtherFields() throws Exception {
        long id = create("2026-2027");
        mvc.perform(put("/periodes/{id}", id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"libelle":"Ancien client modifié","dateDebut":"2026-09-01","dateFin":"2026-12-31","statut":"OUVERTE"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.anneeScolaire").value("2026-2027"))
                .andExpect(jsonPath("$.libelle").value("Ancien client modifié"));
        assertReloaded(id, "2026-2027");
    }

    @Test void clientCannotOverrideYearPresenceWithInternalFlag() throws Exception {
        long id = create("2026-2027");
        String base = """
                {"libelle":"Présence JSON","dateDebut":"2026-09-01","dateFin":"2026-12-31","statut":"OUVERTE",%s}
                """;
        mvc.perform(put("/periodes/{id}", id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(base.formatted("\"anneeScolairePresente\":true")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.anneeScolaire").value("2026-2027"));
        assertReloaded(id, "2026-2027");
        mvc.perform(put("/periodes/{id}", id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(base.formatted("\"anneeScolaire\":null,\"anneeScolairePresente\":false")))
                .andExpect(status().isOk());
        assertReloaded(id, null);
    }

    @Test void malformedJsonShapesStillReturn400() throws Exception {
        for (String body : List.of("[]", "42", "\"texte\"",
                "{\"libelle\":\"Date invalide\",\"dateDebut\":\"incorrect\",\"dateFin\":\"2026-12-31\",\"statut\":\"OUVERTE\"}")) {
            mvc.perform(post("/periodes").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test void readAndWritePermissionsAreUnchangedForEveryRole() throws Exception {
        long id = create("2026-2027");
        for (String role : List.of("ADMIN", "ENSEIGNANT", "RESPONSABLE")) {
            mvc.perform(get("/periodes/{id}", id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.anneeScolaire").value("2026-2027"));
        }
        for (String role : List.of("ENSEIGNANT", "RESPONSABLE")) {
            for (var builder : List.of(post("/periodes"), put("/periodes/{id}", id), delete("/periodes/{id}", id))) {
                mvc.perform(builder.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)))
                                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                        .andExpect(status().isForbidden());
            }
        }
        mvc.perform(get("/periodes/{id}", id)).andExpect(status().isUnauthorized());
        assertReloaded(id, "2026-2027");
    }
}
