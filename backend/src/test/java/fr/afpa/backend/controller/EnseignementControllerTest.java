package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.enseignement.EnseignementRequest;
import fr.afpa.backend.dto.enseignement.EnseignementResponse;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceInUseException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.security.SecurityExpressions;
import fr.afpa.backend.service.EnseignementService;
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

@WebMvcTest(EnseignementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EnseignementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SecurityExpressions securityExpressions;

    @MockitoBean
    private EnseignementService enseignementService;

    private EnseignementRequest request;
    private EnseignementResponse response;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(securityExpressions.peutConsulterEnseignement(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.nullable(org.springframework.security.core.Authentication.class))).thenReturn(true);
        request = new EnseignementRequest(
                1L,
                2L,
                3L,
                BigDecimal.valueOf(2.5),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 6, 30),
                true
        );

        response = new EnseignementResponse(
                10L,
                request.idEnseignant(),
                request.idClasse(),
                request.idMatiere(),
                request.coefficientMatiere(),
                request.dateDebut(),
                request.dateFin(),
                request.actif()
        );
    }

    @Test
    void shouldReturnAllTeachings() throws Exception {
        when(enseignementService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/enseignements"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath(
                        "$[0].idEnseignement"
                ).value(10))
                .andExpect(jsonPath(
                        "$[0].idEnseignant"
                ).value(1))
                .andExpect(jsonPath(
                        "$[0].idClasse"
                ).value(2))
                .andExpect(jsonPath(
                        "$[0].idMatiere"
                ).value(3))
                .andExpect(jsonPath(
                        "$[0].coefficientMatiere"
                ).value(2.5))
                .andExpect(jsonPath(
                        "$[0].actif"
                ).value(true));

        verify(enseignementService).findAll();
    }

    @Test
    void shouldReturnTeachingById() throws Exception {
        when(enseignementService.findById(10L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/enseignements/{id}",
                        10L
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.idEnseignement"
                ).value(10))
                .andExpect(jsonPath(
                        "$.idEnseignant"
                ).value(1))
                .andExpect(jsonPath(
                        "$.idClasse"
                ).value(2))
                .andExpect(jsonPath(
                        "$.idMatiere"
                ).value(3))
                .andExpect(jsonPath(
                        "$.dateDebut"
                ).value("2026-09-01"))
                .andExpect(jsonPath(
                        "$.dateFin"
                ).value("2027-06-30"));

        verify(enseignementService).findById(10L);
    }

    @Test
    void shouldCreateTeaching() throws Exception {
        when(enseignementService.create(
                any(EnseignementRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/enseignements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request
                                )
                        ))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/enseignements/10"
                ))
                .andExpect(jsonPath(
                        "$.idEnseignement"
                ).value(10))
                .andExpect(jsonPath(
                        "$.idEnseignant"
                ).value(1))
                .andExpect(jsonPath(
                        "$.idClasse"
                ).value(2))
                .andExpect(jsonPath(
                        "$.idMatiere"
                ).value(3));

        verify(enseignementService)
                .create(any(EnseignementRequest.class));
    }

    @Test
    void shouldUpdateTeaching() throws Exception {
        when(enseignementService.update(
                eq(10L),
                any(EnseignementRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put(
                        "/enseignements/{id}",
                        10L
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
                        "$.idEnseignement"
                ).value(10))
                .andExpect(jsonPath(
                        "$.coefficientMatiere"
                ).value(2.5))
                .andExpect(jsonPath(
                        "$.actif"
                ).value(true));

        verify(enseignementService).update(
                eq(10L),
                any(EnseignementRequest.class)
        );
    }

    @Test
    void shouldDeleteTeaching() throws Exception {
        doNothing()
                .when(enseignementService)
                .delete(10L);

        mockMvc.perform(delete(
                        "/enseignements/{id}",
                        10L
                ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(enseignementService).delete(10L);
    }

    @Test
    void shouldReturnNotFoundWhenTeachingDoesNotExist()
            throws Exception {

        when(enseignementService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "L'enseignement ayant l'identifiant "
                                + "99 est introuvable."
                ));

        mockMvc.perform(get(
                        "/enseignements/{id}",
                        99L
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
                        "L'enseignement ayant l'identifiant "
                                + "99 est introuvable."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/enseignements/99"));
    }

    @Test
    void shouldReturnConflictWhenTeachingAlreadyExists()
            throws Exception {

        when(enseignementService.create(
                any(EnseignementRequest.class)
        )).thenThrow(new DuplicateResourceException(
                "Un enseignement existe déjà pour l'enseignant "
                        + request.idEnseignant()
                        + ", la classe "
                        + request.idClasse()
                        + ", la matière "
                        + request.idMatiere()
                        + " et la date de début "
                        + request.dateDebut()
                        + "."
        ));

        mockMvc.perform(post("/enseignements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request
                                )
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
                        "Un enseignement existe déjà pour l'enseignant "
                                + request.idEnseignant()
                                + ", la classe "
                                + request.idClasse()
                                + ", la matière "
                                + request.idMatiere()
                                + " et la date de début "
                                + request.dateDebut()
                                + "."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/enseignements"));
    }

    @Test
    void shouldReturnConflictWhenTeachingIsUsed()
            throws Exception {

        doThrow(new ResourceInUseException(
                "Impossible de supprimer l'enseignement ayant l'identifiant "
                        + "10 car il est associé à au moins une évaluation ou une ligne de bulletin."
        ))
                .when(enseignementService)
                .delete(10L);

        mockMvc.perform(delete(
                        "/enseignements/{id}",
                        10L
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
                        "Impossible de supprimer l'enseignement ayant l'identifiant "
                                + "10 car il est associé à au moins une évaluation ou une ligne de bulletin."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/enseignements/10"));

        verify(enseignementService).delete(10L);
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        EnseignementRequest invalidRequest =
                new EnseignementRequest(
                        -1L,
                        0L,
                        -3L,
                        BigDecimal.ZERO,
                        LocalDate.of(2026, 9, 2),
                        LocalDate.of(2026, 9, 1),
                        true
                );

        mockMvc.perform(post("/enseignements")
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
                ).value("/enseignements"))
                .andExpect(jsonPath(
                        "$.validationErrors.idEnseignant"
                ).value(
                        "L'identifiant de l'enseignant doit être positif."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.idClasse"
                ).value(
                        "L'identifiant de la classe doit être positif."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.idMatiere"
                ).value(
                        "L'identifiant de la matière doit être positif."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.coefficientMatiere"
                ).value(
                        "Le coefficient de la matière doit être strictement positif."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.datesEnseignementCoherentes"
                ).value(
                        "La date de fin doit être postérieure ou égale à la date de début."
                ));
    }
}
