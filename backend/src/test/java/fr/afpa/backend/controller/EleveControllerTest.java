package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.eleve.EleveRequest;
import fr.afpa.backend.dto.eleve.EleveResponse;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.security.SecurityExpressions;
import fr.afpa.backend.service.EleveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EleveController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EleveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SecurityExpressions securityExpressions;

    @MockitoBean
    private EleveService eleveService;

    private EleveRequest request;
    private EleveResponse response;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(securityExpressions.peutConsulterEleve(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.nullable(org.springframework.security.core.Authentication.class))).thenReturn(true);
        request = new EleveRequest(
                "Dupont",
                "Alice",
                "alice.dupont@example.com",
                "0612345678",
                "10 rue de Paris",
                "ELEVE-001",
                LocalDate.of(2012, 5, 15),
                null
        );

        response = new EleveResponse(
                1L,
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.matricule(),
                request.dateNaissance(),
                request.photoUrl()
        );
    }

    @Test
    void shouldReturnAllStudents() throws Exception {
        when(eleveService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/eleves"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].idPersonne").value(1))
                .andExpect(jsonPath("$[0].nom").value("Dupont"))
                .andExpect(jsonPath("$[0].prenom").value("Alice"))
                .andExpect(jsonPath("$[0].matricule")
                        .value("ELEVE-001"));

        verify(eleveService).findAll();
    }

    @Test
    void shouldReturnStudentById() throws Exception {
        when(eleveService.findById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/eleves/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPersonne").value(1))
                .andExpect(jsonPath("$.nom").value("Dupont"))
                .andExpect(jsonPath("$.prenom").value("Alice"))
                .andExpect(jsonPath("$.matricule")
                        .value("ELEVE-001"))
                .andExpect(jsonPath("$.dateNaissance")
                        .value("2012-05-15"));

        verify(eleveService).findById(1L);
    }

    @Test
    void shouldCreateStudent() throws Exception {
        when(eleveService.create(any(EleveRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/eleves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/eleves/1"
                ))
                .andExpect(jsonPath("$.idPersonne").value(1))
                .andExpect(jsonPath("$.nom").value("Dupont"))
                .andExpect(jsonPath("$.matricule")
                        .value("ELEVE-001"));

        verify(eleveService)
                .create(any(EleveRequest.class));
    }

    @Test
    void shouldUpdateStudent() throws Exception {
        when(eleveService.update(
                org.mockito.ArgumentMatchers.eq(1L),
                any(EleveRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/eleves/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPersonne").value(1))
                .andExpect(jsonPath("$.nom").value("Dupont"))
                .andExpect(jsonPath("$.matricule")
                        .value("ELEVE-001"));

        verify(eleveService).update(
                org.mockito.ArgumentMatchers.eq(1L),
                any(EleveRequest.class)
        );
    }

    @Test
    void shouldDeleteStudent() throws Exception {
        doNothing()
                .when(eleveService)
                .delete(1L);

        mockMvc.perform(delete("/eleves/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(eleveService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenStudentDoesNotExist()
            throws Exception {

        when(eleveService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "L'élève ayant l'identifiant 99 est introuvable."
                ));

        mockMvc.perform(get("/eleves/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "L'élève ayant l'identifiant 99 est introuvable."
                ))
                .andExpect(jsonPath("$.path")
                        .value("/eleves/99"));
    }

    @Test
    void shouldReturnConflictWhenMatriculeAlreadyExists()
            throws Exception {

        when(eleveService.create(any(EleveRequest.class)))
                .thenThrow(new DuplicateResourceException(
                        "Un élève possède déjà le matricule "
                                + request.matricule()
                                + "."
                ));

        mockMvc.perform(post("/eleves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Un élève possède déjà le matricule "
                                + request.matricule()
                                + "."
                ))
                .andExpect(jsonPath("$.path")
                        .value("/eleves"));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        EleveRequest invalidRequest = new EleveRequest(
                "",
                "",
                "adresse-invalide",
                null,
                null,
                "",
                LocalDate.now().plusDays(1),
                null
        );

        mockMvc.perform(post("/eleves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                invalidRequest
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "Les données envoyées sont invalides."
                ))
                .andExpect(jsonPath("$.path")
                        .value("/eleves"))
                .andExpect(jsonPath(
                        "$.validationErrors.nom"
                ).value("Le nom est obligatoire."))
                .andExpect(jsonPath(
                        "$.validationErrors.prenom"
                ).value("Le prénom est obligatoire."))
                .andExpect(jsonPath(
                        "$.validationErrors.matricule"
                ).value("Le matricule est obligatoire."))
                .andExpect(jsonPath(
                        "$.validationErrors.emailContact"
                ).value("L'adresse e-mail n'est pas valide."))
                .andExpect(jsonPath(
                        "$.validationErrors.dateNaissance"
                ).value(
                        "La date de naissance doit être antérieure à aujourd'hui."
                ));
    }
}
