package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.evaluation.EvaluationRequest;
import fr.afpa.backend.dto.evaluation.EvaluationResponse;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceInUseException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.security.SecurityExpressions;
import fr.afpa.backend.service.EvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

@WebMvcTest(EvaluationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SecurityExpressions securityExpressions;

    @MockitoBean
    private EvaluationService evaluationService;

    private EvaluationRequest request;
    private EvaluationResponse response;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(securityExpressions.peutConsulterEvaluation(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.nullable(org.springframework.security.core.Authentication.class))).thenReturn(true);
        request = new EvaluationRequest(
                10L,
                2L,
                "Contrôle de mathématiques",
                LocalDate.of(2026, 10, 15),
                "CONTROLE",
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(20)
        );

        response = new EvaluationResponse(
                100L,
                request.idEnseignement(),
                request.idPeriode(),
                request.libelle(),
                request.dateEvaluation(),
                request.typeEvaluation(),
                request.coefficientEvaluation(),
                request.bareme()
        );
    }

    @Test
    void shouldReturnAllEvaluations() throws Exception {
        when(evaluationService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/evaluations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath(
                        "$[0].idEvaluation"
                ).value(100))
                .andExpect(jsonPath(
                        "$[0].idEnseignement"
                ).value(10))
                .andExpect(jsonPath(
                        "$[0].idPeriode"
                ).value(2))
                .andExpect(jsonPath(
                        "$[0].libelle"
                ).value("Contrôle de mathématiques"))
                .andExpect(jsonPath(
                        "$[0].dateEvaluation"
                ).value("2026-10-15"))
                .andExpect(jsonPath(
                        "$[0].typeEvaluation"
                ).value("CONTROLE"))
                .andExpect(jsonPath(
                        "$[0].coefficientEvaluation"
                ).value(2))
                .andExpect(jsonPath(
                        "$[0].bareme"
                ).value(20));

        verify(evaluationService).findAll();
    }

    @Test
    void shouldReturnEvaluationById() throws Exception {
        when(evaluationService.findById(100L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/evaluations/{id}",
                        100L
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.idEvaluation"
                ).value(100))
                .andExpect(jsonPath(
                        "$.idEnseignement"
                ).value(10))
                .andExpect(jsonPath(
                        "$.idPeriode"
                ).value(2))
                .andExpect(jsonPath(
                        "$.libelle"
                ).value("Contrôle de mathématiques"))
                .andExpect(jsonPath(
                        "$.dateEvaluation"
                ).value("2026-10-15"))
                .andExpect(jsonPath(
                        "$.typeEvaluation"
                ).value("CONTROLE"))
                .andExpect(jsonPath(
                        "$.coefficientEvaluation"
                ).value(2))
                .andExpect(jsonPath(
                        "$.bareme"
                ).value(20));

        verify(evaluationService).findById(100L);
    }

    @Test
    void shouldCreateEvaluation() throws Exception {
        when(evaluationService.create(
                any(EvaluationRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request
                                )
                        ))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/evaluations/100"
                ))
                .andExpect(jsonPath(
                        "$.idEvaluation"
                ).value(100))
                .andExpect(jsonPath(
                        "$.idEnseignement"
                ).value(10))
                .andExpect(jsonPath(
                        "$.idPeriode"
                ).value(2))
                .andExpect(jsonPath(
                        "$.libelle"
                ).value("Contrôle de mathématiques"));

        verify(evaluationService)
                .create(any(EvaluationRequest.class));
    }

    @Test
    void shouldUpdateEvaluation() throws Exception {
        when(evaluationService.update(
                eq(100L),
                any(EvaluationRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put(
                        "/evaluations/{id}",
                        100L
                )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                objectMapper.writeValueAsString(
                                        request
                                )
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.idEvaluation"
                ).value(100))
                .andExpect(jsonPath(
                        "$.libelle"
                ).value("Contrôle de mathématiques"))
                .andExpect(jsonPath(
                        "$.coefficientEvaluation"
                ).value(2))
                .andExpect(jsonPath(
                        "$.bareme"
                ).value(20));

        verify(evaluationService).update(
                eq(100L),
                any(EvaluationRequest.class)
        );
    }

    @Test
    void shouldDeleteEvaluation() throws Exception {
        doNothing()
                .when(evaluationService)
                .delete(100L);

        mockMvc.perform(delete(
                        "/evaluations/{id}",
                        100L
                ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(evaluationService).delete(100L);
    }

    @Test
    void shouldReturnNotFoundWhenEvaluationDoesNotExist()
            throws Exception {

        when(evaluationService.findById(999L))
                .thenThrow(new ResourceNotFoundException(
                        "L'évaluation ayant l'identifiant "
                                + "999 est introuvable."
                ));

        mockMvc.perform(get(
                        "/evaluations/{id}",
                        999L
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(
                        "$.status"
                ).value(404))
                .andExpect(jsonPath(
                        "$.error"
                ).value("Not Found"))
                .andExpect(jsonPath(
                        "$.message"
                ).value(
                        "L'évaluation ayant l'identifiant "
                                + "999 est introuvable."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/evaluations/999"));
    }

    @Test
    void shouldReturnConflictWhenEvaluationIsUsed()
            throws Exception {

        doThrow(new ResourceInUseException(
                "Impossible de supprimer l'évaluation ayant l'identifiant "
                        + "100 car elle est associée à au moins une note."
        ))
                .when(evaluationService)
                .delete(100L);

        mockMvc.perform(delete(
                        "/evaluations/{id}",
                        100L
                ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(
                        "$.status"
                ).value(409))
                .andExpect(jsonPath(
                        "$.error"
                ).value("Conflict"))
                .andExpect(jsonPath(
                        "$.message"
                ).value(
                        "Impossible de supprimer l'évaluation ayant l'identifiant "
                                + "100 car elle est associée à au moins une note."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/evaluations/100"));

        verify(evaluationService).delete(100L);
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        EvaluationRequest invalidRequest =
                new EvaluationRequest(
                        -1L,
                        0L,
                        "",
                        null,
                        "",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

        mockMvc.perform(post("/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        invalidRequest
                                )
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.status"
                ).value(400))
                .andExpect(jsonPath(
                        "$.error"
                ).value("Bad Request"))
                .andExpect(jsonPath(
                        "$.message"
                ).value(
                        "Les données envoyées sont invalides."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/evaluations"))
                .andExpect(jsonPath(
                        "$.validationErrors.idEnseignement"
                ).value(
                        "L'identifiant de l'enseignement doit être positif."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.idPeriode"
                ).value(
                        "L'identifiant de la période doit être positif."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.libelle"
                ).value(
                        "Le libellé de l'évaluation est obligatoire."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.dateEvaluation"
                ).value(
                        "La date de l'évaluation est obligatoire."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.typeEvaluation"
                ).value(
                        "Le type de l'évaluation est obligatoire."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.coefficientEvaluation"
                ).value(
                        "Le coefficient de l'évaluation doit être strictement positif."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.bareme"
                ).value(
                        "Le barème de l'évaluation doit être strictement positif."
                ));
    }
}
