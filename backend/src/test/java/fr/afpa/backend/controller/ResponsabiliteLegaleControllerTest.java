package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.responsabilitelegale.ResponsabiliteLegaleRequest;
import fr.afpa.backend.dto.responsabilitelegale.ResponsabiliteLegaleResponse;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.ResponsabiliteLegaleService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResponsabiliteLegaleController.class)
@Import(GlobalExceptionHandler.class)
class ResponsabiliteLegaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ResponsabiliteLegaleService
            responsabiliteLegaleService;

    private ResponsabiliteLegaleRequest request;
    private ResponsabiliteLegaleResponse response;

    @BeforeEach
    void setUp() {
        request = new ResponsabiliteLegaleRequest(
                1L,
                2L
        );

        response = new ResponsabiliteLegaleResponse(
                1L,
                "Dupont",
                "Claire",
                2L,
                "Martin",
                "Lucas"
        );
    }

    @Test
    void shouldReturnAllLegalResponsibilities()
            throws Exception {

        when(responsabiliteLegaleService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get(
                        "/responsabilites-legales"
                ))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath(
                        "$[0].idResponsable"
                ).value(1))
                .andExpect(jsonPath(
                        "$[0].nomResponsable"
                ).value("Dupont"))
                .andExpect(jsonPath(
                        "$[0].idEleve"
                ).value(2))
                .andExpect(jsonPath(
                        "$[0].nomEleve"
                ).value("Martin"));

        verify(responsabiliteLegaleService).findAll();
    }

    @Test
    void shouldReturnLegalResponsibilityById()
            throws Exception {

        when(responsabiliteLegaleService.findById(
                1L,
                2L
        )).thenReturn(response);

        mockMvc.perform(get(
                        "/responsabilites-legales/{idResponsable}/{idEleve}",
                        1L,
                        2L
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.idResponsable"
                ).value(1))
                .andExpect(jsonPath(
                        "$.prenomResponsable"
                ).value("Claire"))
                .andExpect(jsonPath(
                        "$.idEleve"
                ).value(2))
                .andExpect(jsonPath(
                        "$.prenomEleve"
                ).value("Lucas"));

        verify(responsabiliteLegaleService).findById(
                1L,
                2L
        );
    }

    @Test
    void shouldCreateLegalResponsibility()
            throws Exception {

        when(responsabiliteLegaleService.create(
                any(ResponsabiliteLegaleRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post(
                        "/responsabilites-legales"
                )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                objectMapper.writeValueAsString(
                                        request
                                )
                        ))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/responsabilites-legales/1/2"
                ))
                .andExpect(jsonPath(
                        "$.idResponsable"
                ).value(1))
                .andExpect(jsonPath(
                        "$.idEleve"
                ).value(2));

        verify(responsabiliteLegaleService).create(
                any(ResponsabiliteLegaleRequest.class)
        );
    }

    @Test
    void shouldDeleteLegalResponsibility()
            throws Exception {

        doNothing()
                .when(responsabiliteLegaleService)
                .delete(1L, 2L);

        mockMvc.perform(delete(
                        "/responsabilites-legales/{idResponsable}/{idEleve}",
                        1L,
                        2L
                ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(responsabiliteLegaleService).delete(
                1L,
                2L
        );
    }

    @Test
    void shouldReturnNotFoundWhenLegalResponsibilityDoesNotExist()
            throws Exception {

        when(responsabiliteLegaleService.findById(
                1L,
                2L
        )).thenThrow(new ResourceNotFoundException(
                "La responsabilité légale entre le responsable "
                        + "1 et l'élève 2 est introuvable."
        ));

        mockMvc.perform(get(
                        "/responsabilites-legales/{idResponsable}/{idEleve}",
                        1L,
                        2L
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
                        "La responsabilité légale entre le responsable "
                                + "1 et l'élève 2 est introuvable."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value(
                        "/responsabilites-legales/1/2"
                ));
    }

    @Test
    void shouldReturnConflictWhenLegalResponsibilityAlreadyExists()
            throws Exception {

        when(responsabiliteLegaleService.create(
                any(ResponsabiliteLegaleRequest.class)
        )).thenThrow(new DuplicateResourceException(
                "La responsabilité légale entre le responsable "
                        + "1 et l'élève 2 existe déjà."
        ));

        mockMvc.perform(post(
                        "/responsabilites-legales"
                )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
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
                        "La responsabilité légale entre le responsable "
                                + "1 et l'élève 2 existe déjà."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/responsabilites-legales"));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        ResponsabiliteLegaleRequest invalidRequest =
                new ResponsabiliteLegaleRequest(
                        null,
                        0L
                );

        mockMvc.perform(post(
                        "/responsabilites-legales"
                )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
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
                ).value("/responsabilites-legales"))
                .andExpect(jsonPath(
                        "$.validationErrors.idResponsable"
                ).value(
                        "L'identifiant du responsable est obligatoire."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.idEleve"
                ).value(
                        "L'identifiant de l'élève doit être strictement positif."
                ));
    }
}
