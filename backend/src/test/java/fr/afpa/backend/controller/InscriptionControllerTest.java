package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.inscription.InscriptionRequest;
import fr.afpa.backend.dto.inscription.InscriptionResponse;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.InscriptionService;
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

@WebMvcTest(InscriptionController.class)
@Import(GlobalExceptionHandler.class)
class InscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InscriptionService inscriptionService;

    private InscriptionRequest request;
    private InscriptionResponse response;

    @BeforeEach
    void setUp() {
        request = new InscriptionRequest(
                2L,
                LocalDate.of(2026, 8, 15),
                null,
                "ACTIVE"
        );

        response = new InscriptionResponse(
                1L,
                request.idEleve(),
                request.dateInscription(),
                request.dateFin(),
                request.statut()
        );
    }

    @Test
    void shouldReturnAllInscriptions() throws Exception {
        when(inscriptionService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/inscriptions"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].idInscription")
                        .value(1))
                .andExpect(jsonPath("$[0].idEleve")
                        .value(2))
                .andExpect(jsonPath("$[0].dateInscription")
                        .value("2026-08-15"))
                .andExpect(jsonPath("$[0].dateFin")
                        .doesNotExist())
                .andExpect(jsonPath("$[0].statut")
                        .value("ACTIVE"));

        verify(inscriptionService).findAll();
    }

    @Test
    void shouldReturnInscriptionById() throws Exception {
        when(inscriptionService.findById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/inscriptions/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idInscription")
                        .value(1))
                .andExpect(jsonPath("$.idEleve")
                        .value(2))
                .andExpect(jsonPath("$.dateInscription")
                        .value("2026-08-15"))
                .andExpect(jsonPath("$.statut")
                        .value("ACTIVE"));

        verify(inscriptionService).findById(1L);
    }

    @Test
    void shouldCreateInscription() throws Exception {
        when(inscriptionService.create(
                any(InscriptionRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/inscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/inscriptions/1"
                ))
                .andExpect(jsonPath("$.idInscription")
                        .value(1))
                .andExpect(jsonPath("$.idEleve")
                        .value(2))
                .andExpect(jsonPath("$.dateInscription")
                        .value("2026-08-15"))
                .andExpect(jsonPath("$.statut")
                        .value("ACTIVE"));

        verify(inscriptionService)
                .create(any(InscriptionRequest.class));
    }

    @Test
    void shouldUpdateInscription() throws Exception {
        InscriptionRequest updateRequest =
                new InscriptionRequest(
                        2L,
                        LocalDate.of(2026, 8, 15),
                        LocalDate.of(2027, 6, 30),
                        "TERMINEE"
                );

        InscriptionResponse updateResponse =
                new InscriptionResponse(
                        1L,
                        updateRequest.idEleve(),
                        updateRequest.dateInscription(),
                        updateRequest.dateFin(),
                        updateRequest.statut()
                );

        when(inscriptionService.update(
                eq(1L),
                any(InscriptionRequest.class)
        )).thenReturn(updateResponse);

        mockMvc.perform(put("/inscriptions/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                updateRequest
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idInscription")
                        .value(1))
                .andExpect(jsonPath("$.dateFin")
                        .value("2027-06-30"))
                .andExpect(jsonPath("$.statut")
                        .value("TERMINEE"));

        verify(inscriptionService).update(
                eq(1L),
                any(InscriptionRequest.class)
        );
    }

    @Test
    void shouldDeleteInscription() throws Exception {
        doNothing()
                .when(inscriptionService)
                .delete(1L);

        mockMvc.perform(delete("/inscriptions/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(inscriptionService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenInscriptionDoesNotExist()
            throws Exception {

        when(inscriptionService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "L'inscription ayant l'identifiant "
                                + "99 est introuvable."
                ));

        mockMvc.perform(get("/inscriptions/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "L'inscription ayant l'identifiant "
                                        + "99 est introuvable."
                        ))
                .andExpect(jsonPath("$.path")
                        .value("/inscriptions/99"));
    }

    @Test
    void shouldReturnNotFoundWhenStudentDoesNotExist()
            throws Exception {

        when(inscriptionService.create(
                any(InscriptionRequest.class)
        )).thenThrow(new ResourceNotFoundException(
                "L'élève ayant l'identifiant "
                        + "2 est introuvable."
        ));

        mockMvc.perform(post("/inscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "L'élève ayant l'identifiant "
                                        + "2 est introuvable."
                        ))
                .andExpect(jsonPath("$.path")
                        .value("/inscriptions"));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        InscriptionRequest invalidRequest =
                new InscriptionRequest(
                        0L,
                        LocalDate.of(2026, 9, 2),
                        LocalDate.of(2026, 9, 1),
                        ""
                );

        mockMvc.perform(post("/inscriptions")
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
                        .value("/inscriptions"))
                .andExpect(jsonPath(
                        "$.validationErrors.idEleve"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.statut"
                ).exists())
                .andExpect(jsonPath(
                        "$.validationErrors.datesInscriptionCoherentes"
                ).exists());
    }
}