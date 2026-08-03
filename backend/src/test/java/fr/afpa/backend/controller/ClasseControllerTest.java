package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.classe.ClasseRequest;
import fr.afpa.backend.dto.classe.ClasseResponse;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.ClasseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(ClasseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ClasseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClasseService classeService;

    private ClasseRequest request;
    private ClasseResponse response;

    @BeforeEach
    void setUp() {
        request = new ClasseRequest(
                "6A",
                "Sixième",
                "2026-2027",
                10L
        );

        response = new ClasseResponse(
                1L,
                request.nom(),
                request.niveau(),
                request.anneeScolaire(),
                request.idProfesseurPrincipal()
        );
    }

    @Test
    void shouldReturnAllClasses() throws Exception {
        when(classeService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/classes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].idClasse").value(1))
                .andExpect(jsonPath("$[0].nom").value("6A"))
                .andExpect(jsonPath("$[0].niveau")
                        .value("Sixième"))
                .andExpect(jsonPath("$[0].anneeScolaire")
                        .value("2026-2027"))
                .andExpect(jsonPath(
                        "$[0].idProfesseurPrincipal"
                ).value(10));

        verify(classeService).findAll();
    }

    @Test
    void shouldReturnClassById() throws Exception {
        when(classeService.findById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/classes/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idClasse").value(1))
                .andExpect(jsonPath("$.nom").value("6A"))
                .andExpect(jsonPath("$.niveau")
                        .value("Sixième"))
                .andExpect(jsonPath("$.anneeScolaire")
                        .value("2026-2027"))
                .andExpect(jsonPath(
                        "$.idProfesseurPrincipal"
                ).value(10));

        verify(classeService).findById(1L);
    }

    @Test
    void shouldCreateClass() throws Exception {
        when(classeService.create(any(ClasseRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/classes/1"
                ))
                .andExpect(jsonPath("$.idClasse").value(1))
                .andExpect(jsonPath("$.nom").value("6A"))
                .andExpect(jsonPath("$.anneeScolaire")
                        .value("2026-2027"));

        verify(classeService)
                .create(any(ClasseRequest.class));
    }

    @Test
    void shouldUpdateClass() throws Exception {
        when(classeService.update(
                eq(1L),
                any(ClasseRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/classes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idClasse").value(1))
                .andExpect(jsonPath("$.nom").value("6A"))
                .andExpect(jsonPath("$.niveau")
                        .value("Sixième"));

        verify(classeService).update(
                eq(1L),
                any(ClasseRequest.class)
        );
    }

    @Test
    void shouldDeleteClass() throws Exception {
        doNothing()
                .when(classeService)
                .delete(1L);

        mockMvc.perform(delete("/classes/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(classeService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenClassDoesNotExist()
            throws Exception {

        when(classeService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "La classe ayant l'identifiant 99 est introuvable."
                ));

        mockMvc.perform(get("/classes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "La classe ayant l'identifiant 99 est introuvable."
                ))
                .andExpect(jsonPath("$.path")
                        .value("/classes/99"));
    }

    @Test
    void shouldReturnConflictWhenClassAlreadyExists()
            throws Exception {

        when(classeService.create(any(ClasseRequest.class)))
                .thenThrow(new DuplicateResourceException(
                        "Une classe existe deja."
                ));

        mockMvc.perform(post("/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Une classe existe deja."))
                .andExpect(jsonPath("$.path")
                        .value("/classes"));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        ClasseRequest invalidRequest = new ClasseRequest(
                "",
                "",
                "",
                0L
        );

        mockMvc.perform(post("/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                invalidRequest
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.path")
                        .value("/classes"))
                .andExpect(jsonPath(
                        "$.validationErrors.nom"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.niveau"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.anneeScolaire"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.idProfesseurPrincipal"
                ).exists());
    }
}
