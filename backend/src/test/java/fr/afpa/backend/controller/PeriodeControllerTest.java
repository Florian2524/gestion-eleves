package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.periode.PeriodeRequest;
import fr.afpa.backend.dto.periode.PeriodeResponse;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.PeriodeService;
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

@WebMvcTest(PeriodeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PeriodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PeriodeService periodeService;

    private PeriodeRequest request;
    private PeriodeResponse response;

    @BeforeEach
    void setUp() {
        request = new PeriodeRequest(
                "Trimestre 1",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 18),
                LocalDate.of(2026, 11, 30),
                LocalDate.of(2026, 12, 15),
                "OUVERTE"
        );

        response = new PeriodeResponse(
                1L,
                request.libelle(),
                request.dateDebut(),
                request.dateFin(),
                request.dateDebutSaisie(),
                request.dateFinSaisie(),
                request.statut()
        );
    }

    @Test
    void shouldReturnAllPeriods() throws Exception {
        when(periodeService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/periodes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].idPeriode")
                        .value(1))
                .andExpect(jsonPath("$[0].libelle")
                        .value("Trimestre 1"))
                .andExpect(jsonPath("$[0].dateDebut")
                        .value("2026-09-01"))
                .andExpect(jsonPath("$[0].dateFin")
                        .value("2026-12-18"))
                .andExpect(jsonPath("$[0].statut")
                        .value("OUVERTE"));

        verify(periodeService).findAll();
    }

    @Test
    void shouldReturnPeriodById() throws Exception {
        when(periodeService.findById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/periodes/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPeriode")
                        .value(1))
                .andExpect(jsonPath("$.libelle")
                        .value("Trimestre 1"))
                .andExpect(jsonPath("$.dateDebutSaisie")
                        .value("2026-11-30"))
                .andExpect(jsonPath("$.dateFinSaisie")
                        .value("2026-12-15"))
                .andExpect(jsonPath("$.statut")
                        .value("OUVERTE"));

        verify(periodeService).findById(1L);
    }

    @Test
    void shouldCreatePeriod() throws Exception {
        when(periodeService.create(
                any(PeriodeRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/periodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/periodes/1"
                ))
                .andExpect(jsonPath("$.idPeriode")
                        .value(1))
                .andExpect(jsonPath("$.libelle")
                        .value("Trimestre 1"))
                .andExpect(jsonPath("$.statut")
                        .value("OUVERTE"));

        verify(periodeService)
                .create(any(PeriodeRequest.class));
    }

    @Test
    void shouldUpdatePeriod() throws Exception {
        when(periodeService.update(
                eq(1L),
                any(PeriodeRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/periodes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPeriode")
                        .value(1))
                .andExpect(jsonPath("$.libelle")
                        .value("Trimestre 1"))
                .andExpect(jsonPath("$.statut")
                        .value("OUVERTE"));

        verify(periodeService).update(
                eq(1L),
                any(PeriodeRequest.class)
        );
    }

    @Test
    void shouldDeletePeriod() throws Exception {
        doNothing()
                .when(periodeService)
                .delete(1L);

        mockMvc.perform(delete("/periodes/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(periodeService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenPeriodDoesNotExist()
            throws Exception {

        when(periodeService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "La période ayant l'identifiant 99 est introuvable."
                ));

        mockMvc.perform(get("/periodes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "La période ayant l'identifiant 99 est introuvable."
                ))
                .andExpect(jsonPath("$.path")
                        .value("/periodes/99"));
    }

    @Test
    void shouldReturnBadRequestWhenRequiredDataIsMissing()
            throws Exception {

        PeriodeRequest invalidRequest =
                new PeriodeRequest(
                        "",
                        null,
                        null,
                        null,
                        null,
                        ""
                );

        mockMvc.perform(post("/periodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                invalidRequest
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "Les données envoyées sont invalides."
                ))
                .andExpect(jsonPath("$.path")
                        .value("/periodes"))
                .andExpect(jsonPath(
                        "$.validationErrors.libelle"
                ).value("Le libellé est obligatoire."))
                .andExpect(jsonPath(
                        "$.validationErrors.dateDebut"
                ).value("La date de début est obligatoire."))
                .andExpect(jsonPath(
                        "$.validationErrors.dateFin"
                ).value("La date de fin est obligatoire."))
                .andExpect(jsonPath(
                        "$.validationErrors.statut"
                ).value("Le statut est obligatoire."));
    }

    @Test
    void shouldRejectInconsistentPeriodDates()
            throws Exception {

        PeriodeRequest invalidRequest =
                new PeriodeRequest(
                        "Trimestre invalide",
                        LocalDate.of(2026, 12, 18),
                        LocalDate.of(2026, 9, 1),
                        null,
                        null,
                        "OUVERTE"
                );

        mockMvc.perform(post("/periodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                invalidRequest
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath(
                        "$.validationErrors.datesPeriodeCoherentes"
                ).value(
                        "La date de fin doit être postérieure "
                                + "ou égale à la date de début."
                ));

        verify(periodeService, org.mockito.Mockito.never())
                .create(any(PeriodeRequest.class));
    }

    @Test
    void shouldRejectInconsistentEntryDates()
            throws Exception {

        PeriodeRequest invalidRequest =
                new PeriodeRequest(
                        "Trimestre 1",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 12, 18),
                        LocalDate.of(2026, 12, 15),
                        LocalDate.of(2026, 11, 30),
                        "OUVERTE"
                );

        mockMvc.perform(post("/periodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                invalidRequest
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath(
                        "$.validationErrors.datesSaisieCoherentes"
                ).value(
                        "La date de fin de saisie doit être postérieure "
                                + "ou égale à la date de début de saisie."
                ));

        verify(periodeService, org.mockito.Mockito.never())
                .create(any(PeriodeRequest.class));
    }
}
