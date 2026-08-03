package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.note.NoteRequest;
import fr.afpa.backend.dto.note.NoteResponse;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.NoteService;
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
import java.time.OffsetDateTime;
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

@WebMvcTest(NoteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoteService noteService;

    private NoteRequest request;
    private NoteResponse response;

    @BeforeEach
    void setUp() {
        request = new NoteRequest(
                20L,
                100L,
                BigDecimal.valueOf(15.5),
                "Bon travail.",
                "SAISIE"
        );

        response = new NoteResponse(
                200L,
                request.idScolarite(),
                request.idEvaluation(),
                request.valeur(),
                request.commentaire(),
                request.statutNote(),
                OffsetDateTime.parse(
                        "2026-10-15T14:30:00+02:00"
                )
        );
    }

    @Test
    void shouldReturnAllNotes() throws Exception {
        when(noteService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/notes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath(
                        "$[0].idNote"
                ).value(200))
                .andExpect(jsonPath(
                        "$[0].idScolarite"
                ).value(20))
                .andExpect(jsonPath(
                        "$[0].idEvaluation"
                ).value(100))
                .andExpect(jsonPath(
                        "$[0].valeur"
                ).value(15.5))
                .andExpect(jsonPath(
                        "$[0].commentaire"
                ).value("Bon travail."))
                .andExpect(jsonPath(
                        "$[0].statutNote"
                ).value("SAISIE"))
                .andExpect(jsonPath(
                        "$[0].dateSaisie"
                ).value(
                        "2026-10-15T14:30:00+02:00"
                ));

        verify(noteService).findAll();
    }

    @Test
    void shouldReturnNoteById() throws Exception {
        when(noteService.findById(200L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/notes/{id}",
                        200L
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.idNote"
                ).value(200))
                .andExpect(jsonPath(
                        "$.idScolarite"
                ).value(20))
                .andExpect(jsonPath(
                        "$.idEvaluation"
                ).value(100))
                .andExpect(jsonPath(
                        "$.valeur"
                ).value(15.5))
                .andExpect(jsonPath(
                        "$.commentaire"
                ).value("Bon travail."))
                .andExpect(jsonPath(
                        "$.statutNote"
                ).value("SAISIE"));

        verify(noteService).findById(200L);
    }

    @Test
    void shouldCreateNote() throws Exception {
        when(noteService.create(
                any(NoteRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request
                                )
                        ))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/notes/200"
                ))
                .andExpect(jsonPath(
                        "$.idNote"
                ).value(200))
                .andExpect(jsonPath(
                        "$.idScolarite"
                ).value(20))
                .andExpect(jsonPath(
                        "$.idEvaluation"
                ).value(100))
                .andExpect(jsonPath(
                        "$.valeur"
                ).value(15.5))
                .andExpect(jsonPath(
                        "$.statutNote"
                ).value("SAISIE"));

        verify(noteService)
                .create(any(NoteRequest.class));
    }

    @Test
    void shouldUpdateNote() throws Exception {
        when(noteService.update(
                eq(200L),
                any(NoteRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put(
                        "/notes/{id}",
                        200L
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
                        "$.idNote"
                ).value(200))
                .andExpect(jsonPath(
                        "$.valeur"
                ).value(15.5))
                .andExpect(jsonPath(
                        "$.commentaire"
                ).value("Bon travail."))
                .andExpect(jsonPath(
                        "$.statutNote"
                ).value("SAISIE"));

        verify(noteService).update(
                eq(200L),
                any(NoteRequest.class)
        );
    }

    @Test
    void shouldDeleteNote() throws Exception {
        doNothing()
                .when(noteService)
                .delete(200L);

        mockMvc.perform(delete(
                        "/notes/{id}",
                        200L
                ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(noteService).delete(200L);
    }

    @Test
    void shouldReturnNotFoundWhenNoteDoesNotExist()
            throws Exception {

        when(noteService.findById(999L))
                .thenThrow(new ResourceNotFoundException(
                        "La note ayant l'identifiant "
                                + "999 est introuvable."
                ));

        mockMvc.perform(get(
                        "/notes/{id}",
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
                        "La note ayant l'identifiant "
                                + "999 est introuvable."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/notes/999"));

        verify(noteService).findById(999L);
    }

    @Test
    void shouldReturnConflictWhenNoteAlreadyExists()
            throws Exception {

        when(noteService.create(
                any(NoteRequest.class)
        )).thenThrow(new DuplicateResourceException(
                "Une note existe déjà pour la scolarité "
                        + "20 et l'évaluation 100."
        ));

        mockMvc.perform(post("/notes")
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
                        "Une note existe déjà pour la scolarité "
                                + "20 et l'évaluation 100."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/notes"));

        verify(noteService)
                .create(any(NoteRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid()
            throws Exception {

        NoteRequest invalidRequest =
                new NoteRequest(
                        -1L,
                        0L,
                        BigDecimal.valueOf(-1),
                        null,
                        ""
                );

        mockMvc.perform(post("/notes")
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
                ).value("/notes"))
                .andExpect(jsonPath(
                        "$.validationErrors.idScolarite"
                ).value(
                        "L'identifiant de la scolarité doit être positif."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.idEvaluation"
                ).value(
                        "L'identifiant de l'évaluation doit être positif."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.valeur"
                ).value(
                        "La valeur de la note doit être positive ou nulle."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.statutNote"
                ).value(
                        "Le statut de la note est obligatoire."
                ));
    }
}
