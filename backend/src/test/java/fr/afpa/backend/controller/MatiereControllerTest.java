package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.matiere.MatiereRequest;
import fr.afpa.backend.dto.matiere.MatiereResponse;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.MatiereService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

@WebMvcTest(MatiereController.class)
@Import(GlobalExceptionHandler.class)
class MatiereControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MatiereService matiereService;

    private MatiereRequest request;
    private MatiereResponse response;

    @BeforeEach
    void setUp() {
        request = new MatiereRequest(
                "MATH",
                "Mathématiques"
        );

        response = new MatiereResponse(
                1L,
                request.code(),
                request.nom()
        );
    }

    @Test
    void shouldReturnAllSubjects() throws Exception {
        when(matiereService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/matieres"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].idMatiere")
                        .value(1))
                .andExpect(jsonPath("$[0].code")
                        .value("MATH"))
                .andExpect(jsonPath("$[0].nom")
                        .value("Mathématiques"));

        verify(matiereService).findAll();
    }

    @Test
    void shouldReturnSubjectById() throws Exception {
        when(matiereService.findById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/matieres/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMatiere")
                        .value(1))
                .andExpect(jsonPath("$.code")
                        .value("MATH"))
                .andExpect(jsonPath("$.nom")
                        .value("Mathématiques"));

        verify(matiereService).findById(1L);
    }

    @Test
    void shouldCreateSubject() throws Exception {
        when(matiereService.create(
                any(MatiereRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/matieres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/matieres/1"
                ))
                .andExpect(jsonPath("$.idMatiere")
                        .value(1))
                .andExpect(jsonPath("$.code")
                        .value("MATH"))
                .andExpect(jsonPath("$.nom")
                        .value("Mathématiques"));

        verify(matiereService)
                .create(any(MatiereRequest.class));
    }

    @Test
    void shouldUpdateSubject() throws Exception {
        when(matiereService.update(
                eq(1L),
                any(MatiereRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/matieres/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMatiere")
                        .value(1))
                .andExpect(jsonPath("$.code")
                        .value("MATH"))
                .andExpect(jsonPath("$.nom")
                        .value("Mathématiques"));

        verify(matiereService).update(
                eq(1L),
                any(MatiereRequest.class)
        );
    }

    @Test
    void shouldDeleteSubject() throws Exception {
        doNothing()
                .when(matiereService)
                .delete(1L);

        mockMvc.perform(delete("/matieres/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(matiereService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenSubjectDoesNotExist()
            throws Exception {

        when(matiereService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "La matière ayant l'identifiant 99 est introuvable."
                ));

        mockMvc.perform(get("/matieres/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "La matière ayant l'identifiant 99 est introuvable."
                ))
                .andExpect(jsonPath("$.path")
                        .value("/matieres/99"));
    }

    @Test
    void shouldReturnConflictWhenCodeAlreadyExists()
            throws Exception {

        when(matiereService.create(
                any(MatiereRequest.class)
        )).thenThrow(new DuplicateResourceException(
                "Une matière possède déjà le code MATH."
        ));

        mockMvc.perform(post("/matieres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Une matière possède déjà le code MATH."
                ))
                .andExpect(jsonPath("$.path")
                        .value("/matieres"));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        MatiereRequest invalidRequest =
                new MatiereRequest("", "");

        mockMvc.perform(post("/matieres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                invalidRequest
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.path")
                        .value("/matieres"))
                .andExpect(jsonPath(
                        "$.validationErrors.code"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.nom"
                ).exists());
    }
}