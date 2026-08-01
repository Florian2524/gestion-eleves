package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.scolarite.ScolariteRequest;
import fr.afpa.backend.dto.scolarite.ScolariteResponse;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.ScolariteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest(ScolariteController.class)
@Import(GlobalExceptionHandler.class)
class ScolariteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScolariteService scolariteService;

    private ScolariteRequest request;
    private ScolariteResponse response;

    @BeforeEach
    void setUp() {
        request = new ScolariteRequest(
                1L,
                2L,
                LocalDate.of(2026, 9, 1),
                null,
                "EN_COURS"
        );

        response = new ScolariteResponse(
                3L,
                request.idEleve(),
                request.idClasse(),
                request.dateDebut(),
                request.dateFin(),
                request.statut()
        );
    }

    @Test
    void shouldReturnAllScolarites() throws Exception {
        when(scolariteService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/scolarites"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].idScolarite")
                        .value(3))
                .andExpect(jsonPath("$[0].idEleve")
                        .value(1))
                .andExpect(jsonPath("$[0].idClasse")
                        .value(2))
                .andExpect(jsonPath("$[0].dateDebut")
                        .value("2026-09-01"))
                .andExpect(jsonPath("$[0].statut")
                        .value("EN_COURS"));

        verify(scolariteService).findAll();
    }

    @Test
    void shouldReturnScolariteById() throws Exception {
        when(scolariteService.findById(3L))
                .thenReturn(response);

        mockMvc.perform(get("/scolarites/{id}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idScolarite")
                        .value(3))
                .andExpect(jsonPath("$.idEleve")
                        .value(1))
                .andExpect(jsonPath("$.idClasse")
                        .value(2))
                .andExpect(jsonPath("$.dateDebut")
                        .value("2026-09-01"))
                .andExpect(jsonPath("$.statut")
                        .value("EN_COURS"));

        verify(scolariteService).findById(3L);
    }

    @Test
    void shouldCreateScolarite() throws Exception {
        when(scolariteService.create(
                any(ScolariteRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/scolarites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/scolarites/3"
                ))
                .andExpect(jsonPath("$.idScolarite")
                        .value(3))
                .andExpect(jsonPath("$.idEleve")
                        .value(1))
                .andExpect(jsonPath("$.idClasse")
                        .value(2))
                .andExpect(jsonPath("$.statut")
                        .value("EN_COURS"));

        verify(scolariteService)
                .create(any(ScolariteRequest.class));
    }

    @Test
    void shouldUpdateScolarite() throws Exception {
        when(scolariteService.update(
                eq(3L),
                any(ScolariteRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/scolarites/{id}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idScolarite")
                        .value(3))
                .andExpect(jsonPath("$.idEleve")
                        .value(1))
                .andExpect(jsonPath("$.idClasse")
                        .value(2))
                .andExpect(jsonPath("$.dateDebut")
                        .value("2026-09-01"))
                .andExpect(jsonPath("$.statut")
                        .value("EN_COURS"));

        verify(scolariteService).update(
                eq(3L),
                any(ScolariteRequest.class)
        );
    }

    @Test
    void shouldDeleteScolarite() throws Exception {
        doNothing()
                .when(scolariteService)
                .delete(3L);

        mockMvc.perform(delete("/scolarites/{id}", 3L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(scolariteService).delete(3L);
    }

    @Test
    void shouldReturnNotFoundWhenScolariteDoesNotExist()
            throws Exception {

        when(scolariteService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "La scolarité ayant l'identifiant "
                                + "99 est introuvable."
                ));

        mockMvc.perform(get("/scolarites/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "La scolarité ayant l'identifiant "
                                        + "99 est introuvable."
                        ))
                .andExpect(jsonPath("$.path")
                        .value("/scolarites/99"));
    }

    @Test
    void shouldReturnConflictWhenScolariteAlreadyExists()
            throws Exception {

        when(scolariteService.create(
                any(ScolariteRequest.class)
        )).thenThrow(new DuplicateResourceException(
                "Une scolarité existe déjà."
        ));

        mockMvc.perform(post("/scolarites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Une scolarité existe déjà."
                        ))
                .andExpect(jsonPath("$.path")
                        .value("/scolarites"));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        ScolariteRequest invalidRequest =
                new ScolariteRequest(
                        0L,
                        0L,
                        LocalDate.of(2026, 9, 2),
                        LocalDate.of(2026, 9, 1),
                        ""
                );

        mockMvc.perform(post("/scolarites")
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
                        .value("/scolarites"))
                .andExpect(jsonPath(
                        "$.validationErrors.idEleve"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.idClasse"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.statut"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.datesScolariteCoherentes"
                ).exists());
    }
}