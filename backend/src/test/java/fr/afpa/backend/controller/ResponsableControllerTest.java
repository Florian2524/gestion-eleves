package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.responsable.ResponsableRequest;
import fr.afpa.backend.dto.responsable.ResponsableResponse;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.ResponsableService;
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

@WebMvcTest(ResponsableController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ResponsableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ResponsableService responsableService;

    private ResponsableRequest request;
    private ResponsableResponse response;

    @BeforeEach
    void setUp() {
        request = new ResponsableRequest(
                "Dupont",
                "Claire",
                "claire.dupont@example.com",
                "0612345678",
                "12 rue des Écoles",
                "Médecin"
        );

        response = new ResponsableResponse(
                1L,
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.profession()
        );
    }

    @Test
    void shouldReturnAllGuardians() throws Exception {
        when(responsableService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/responsables"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath(
                        "$[0].idPersonne"
                ).value(1))
                .andExpect(jsonPath(
                        "$[0].nom"
                ).value("Dupont"))
                .andExpect(jsonPath(
                        "$[0].prenom"
                ).value("Claire"))
                .andExpect(jsonPath(
                        "$[0].profession"
                ).value("Médecin"));

        verify(responsableService).findAll();
    }

    @Test
    void shouldReturnGuardianById() throws Exception {
        when(responsableService.findById(1L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/responsables/{id}",
                        1L
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.idPersonne"
                ).value(1))
                .andExpect(jsonPath(
                        "$.nom"
                ).value("Dupont"))
                .andExpect(jsonPath(
                        "$.prenom"
                ).value("Claire"))
                .andExpect(jsonPath(
                        "$.profession"
                ).value("Médecin"));

        verify(responsableService).findById(1L);
    }

    @Test
    void shouldCreateGuardian() throws Exception {
        when(responsableService.create(
                any(ResponsableRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/responsables")
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
                        "/responsables/1"
                ))
                .andExpect(jsonPath(
                        "$.idPersonne"
                ).value(1))
                .andExpect(jsonPath(
                        "$.nom"
                ).value("Dupont"))
                .andExpect(jsonPath(
                        "$.profession"
                ).value("Médecin"));

        verify(responsableService)
                .create(any(ResponsableRequest.class));
    }

    @Test
    void shouldUpdateGuardian() throws Exception {
        when(responsableService.update(
                eq(1L),
                any(ResponsableRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put(
                        "/responsables/{id}",
                        1L
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
                        "$.idPersonne"
                ).value(1))
                .andExpect(jsonPath(
                        "$.nom"
                ).value("Dupont"))
                .andExpect(jsonPath(
                        "$.profession"
                ).value("Médecin"));

        verify(responsableService).update(
                eq(1L),
                any(ResponsableRequest.class)
        );
    }

    @Test
    void shouldDeleteGuardian() throws Exception {
        doNothing()
                .when(responsableService)
                .delete(1L);

        mockMvc.perform(delete(
                        "/responsables/{id}",
                        1L
                ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(responsableService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenGuardianDoesNotExist()
            throws Exception {

        when(responsableService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Le responsable ayant l'identifiant "
                                + "99 est introuvable."
                ));

        mockMvc.perform(get(
                        "/responsables/{id}",
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
                        "Le responsable ayant l'identifiant "
                                + "99 est introuvable."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/responsables/99"));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        ResponsableRequest invalidRequest =
                new ResponsableRequest(
                        "",
                        "",
                        "adresse-invalide",
                        null,
                        null,
                        null
                );

        mockMvc.perform(post("/responsables")
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
                ).value("/responsables"))
                .andExpect(jsonPath(
                        "$.validationErrors.nom"
                ).value("Le nom est obligatoire."))
                .andExpect(jsonPath(
                        "$.validationErrors.prenom"
                ).value(
                        "Le prénom est obligatoire."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.emailContact"
                ).value(
                        "L'adresse e-mail n'est pas valide."
                ));
    }
}
