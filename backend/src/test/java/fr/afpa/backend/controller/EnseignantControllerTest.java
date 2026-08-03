package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.enseignant.EnseignantRequest;
import fr.afpa.backend.dto.enseignant.EnseignantResponse;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceInUseException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.EnseignantService;
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

@WebMvcTest(EnseignantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EnseignantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnseignantService enseignantService;

    private EnseignantRequest request;
    private EnseignantResponse response;

    @BeforeEach
    void setUp() {
        request = new EnseignantRequest(
                "Martin",
                "Sophie",
                "sophie.martin@example.com",
                "0611223344",
                "20 rue des Écoles",
                "ENS-001"
        );

        response = new EnseignantResponse(
                1L,
                request.nom(),
                request.prenom(),
                request.emailContact(),
                request.telephone(),
                request.adresse(),
                request.numeroEmploye()
        );
    }

    @Test
    void shouldReturnAllTeachers() throws Exception {
        when(enseignantService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/enseignants"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath(
                        "$[0].idPersonne"
                ).value(1))
                .andExpect(jsonPath(
                        "$[0].nom"
                ).value("Martin"))
                .andExpect(jsonPath(
                        "$[0].prenom"
                ).value("Sophie"))
                .andExpect(jsonPath(
                        "$[0].numeroEmploye"
                ).value("ENS-001"));

        verify(enseignantService).findAll();
    }

    @Test
    void shouldReturnTeacherById() throws Exception {
        when(enseignantService.findById(1L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/enseignants/{id}",
                        1L
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.idPersonne"
                ).value(1))
                .andExpect(jsonPath(
                        "$.nom"
                ).value("Martin"))
                .andExpect(jsonPath(
                        "$.prenom"
                ).value("Sophie"))
                .andExpect(jsonPath(
                        "$.numeroEmploye"
                ).value("ENS-001"));

        verify(enseignantService).findById(1L);
    }

    @Test
    void shouldCreateTeacher() throws Exception {
        when(enseignantService.create(
                any(EnseignantRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/enseignants")
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
                        "/enseignants/1"
                ))
                .andExpect(jsonPath(
                        "$.idPersonne"
                ).value(1))
                .andExpect(jsonPath(
                        "$.nom"
                ).value("Martin"))
                .andExpect(jsonPath(
                        "$.numeroEmploye"
                ).value("ENS-001"));

        verify(enseignantService)
                .create(any(EnseignantRequest.class));
    }

    @Test
    void shouldUpdateTeacher() throws Exception {
        when(enseignantService.update(
                eq(1L),
                any(EnseignantRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put(
                        "/enseignants/{id}",
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
                ).value("Martin"))
                .andExpect(jsonPath(
                        "$.numeroEmploye"
                ).value("ENS-001"));

        verify(enseignantService).update(
                eq(1L),
                any(EnseignantRequest.class)
        );
    }

    @Test
    void shouldDeleteTeacher() throws Exception {
        doNothing()
                .when(enseignantService)
                .delete(1L);

        mockMvc.perform(delete(
                        "/enseignants/{id}",
                        1L
                ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(enseignantService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenTeacherDoesNotExist()
            throws Exception {

        when(enseignantService.findById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "L'enseignant ayant l'identifiant "
                                + "99 est introuvable."
                ));

        mockMvc.perform(get(
                        "/enseignants/{id}",
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
                        "L'enseignant ayant l'identifiant "
                                + "99 est introuvable."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/enseignants/99"));
    }

    @Test
    void shouldReturnConflictWhenEmployeeNumberAlreadyExists()
            throws Exception {

        when(enseignantService.create(
                any(EnseignantRequest.class)
        )).thenThrow(new DuplicateResourceException(
                "Un enseignant possède déjà le numéro d'employé "
                        + request.numeroEmploye()
                        + "."
        ));

        mockMvc.perform(post("/enseignants")
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
                        "Un enseignant possède déjà le numéro d'employé "
                                + request.numeroEmploye()
                                + "."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/enseignants"));
    }

    @Test
    void shouldReturnConflictWhenTeacherIsUsed()
            throws Exception {

        doThrow(new ResourceInUseException(
                "Impossible de supprimer l'enseignant ayant l'identifiant "
                        + "1 car il est associé à au moins un enseignement."
        ))
                .when(enseignantService)
                .delete(1L);

        mockMvc.perform(delete(
                        "/enseignants/{id}",
                        1L
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
                        "Impossible de supprimer l'enseignant ayant l'identifiant "
                                + "1 car il est associé à au moins un enseignement."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/enseignants/1"));

        verify(enseignantService).delete(1L);
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        EnseignantRequest invalidRequest =
                new EnseignantRequest(
                        "",
                        "",
                        "adresse-invalide",
                        null,
                        null,
                        ""
                );

        mockMvc.perform(post("/enseignants")
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
                ).value("/enseignants"))
                .andExpect(jsonPath(
                        "$.validationErrors.nom"
                ).value("Le nom est obligatoire."))
                .andExpect(jsonPath(
                        "$.validationErrors.prenom"
                ).value(
                        "Le prénom est obligatoire."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.numeroEmploye"
                ).value(
                        "Le numéro d'employé est obligatoire."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.emailContact"
                ).value(
                        "L'adresse e-mail n'est pas valide."
                ));
    }
}
