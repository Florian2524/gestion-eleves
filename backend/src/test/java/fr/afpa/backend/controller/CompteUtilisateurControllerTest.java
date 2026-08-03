package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurCreateRequest;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurResponse;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurUpdateRequest;
import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.CompteUtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(CompteUtilisateurController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CompteUtilisateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompteUtilisateurService
            compteUtilisateurService;

    private CompteUtilisateurCreateRequest createRequest;
    private CompteUtilisateurUpdateRequest updateRequest;
    private CompteUtilisateurResponse response;

    @BeforeEach
    void setUp() {
        createRequest =
                new CompteUtilisateurCreateRequest(
                        1L,
                        "claire.dupont@example.com",
                        "MotDePasse123",
                        RoleUtilisateur.RESPONSABLE
                );

        updateRequest =
                new CompteUtilisateurUpdateRequest(
                        "claire.dupont@ecole.example.com",
                        null,
                        RoleUtilisateur.ADMIN,
                        false
                );

        response =
                new CompteUtilisateurResponse(
                        10L,
                        1L,
                        "Dupont",
                        "Claire",
                        "claire.dupont@example.com",
                        RoleUtilisateur.RESPONSABLE,
                        true,
                        OffsetDateTime.parse(
                                "2026-08-03T10:00:00+02:00"
                        )
                );
    }

    @Test
    void shouldReturnAllUserAccounts()
            throws Exception {

        when(compteUtilisateurService.findAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get(
                        "/comptes-utilisateurs"
                ))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath(
                        "$[0].idUtilisateur"
                ).value(10))
                .andExpect(jsonPath(
                        "$[0].idPersonne"
                ).value(1))
                .andExpect(jsonPath(
                        "$[0].emailConnexion"
                ).value("claire.dupont@example.com"))
                .andExpect(jsonPath(
                        "$[0].role"
                ).value("RESPONSABLE"))
                .andExpect(jsonPath(
                        "$[0].actif"
                ).value(true))
                .andExpect(jsonPath(
                        "$[0].motDePasse"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$[0].motDePasseHash"
                ).doesNotExist());

        verify(compteUtilisateurService).findAll();
    }

    @Test
    void shouldReturnUserAccountById()
            throws Exception {

        when(compteUtilisateurService.findById(10L))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/comptes-utilisateurs/{idUtilisateur}",
                        10L
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.idUtilisateur"
                ).value(10))
                .andExpect(jsonPath(
                        "$.nom"
                ).value("Dupont"))
                .andExpect(jsonPath(
                        "$.prenom"
                ).value("Claire"))
                .andExpect(jsonPath(
                        "$.dateCreation"
                ).exists())
                .andExpect(jsonPath(
                        "$.motDePasse"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$.motDePasseHash"
                ).doesNotExist());

        verify(compteUtilisateurService).findById(10L);
    }

    @Test
    void shouldCreateUserAccount()
            throws Exception {

        when(compteUtilisateurService.create(
                any(CompteUtilisateurCreateRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post(
                        "/comptes-utilisateurs"
                )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                objectMapper.writeValueAsString(
                                        createRequest
                                )
                        ))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/comptes-utilisateurs/10"
                ))
                .andExpect(jsonPath(
                        "$.idUtilisateur"
                ).value(10))
                .andExpect(jsonPath(
                        "$.idPersonne"
                ).value(1))
                .andExpect(jsonPath(
                        "$.motDePasse"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$.motDePasseHash"
                ).doesNotExist());

        verify(compteUtilisateurService).create(
                any(CompteUtilisateurCreateRequest.class)
        );
    }

    @Test
    void shouldUpdateUserAccount()
            throws Exception {

        CompteUtilisateurResponse updatedResponse =
                new CompteUtilisateurResponse(
                        10L,
                        1L,
                        "Dupont",
                        "Claire",
                        "claire.dupont@ecole.example.com",
                        RoleUtilisateur.ADMIN,
                        false,
                        response.dateCreation()
                );

        when(compteUtilisateurService.update(
                any(Long.class),
                any(CompteUtilisateurUpdateRequest.class)
        )).thenReturn(updatedResponse);

        mockMvc.perform(put(
                        "/comptes-utilisateurs/{idUtilisateur}",
                        10L
                )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                objectMapper.writeValueAsString(
                                        updateRequest
                                )
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.emailConnexion"
                ).value(
                        "claire.dupont@ecole.example.com"
                ))
                .andExpect(jsonPath(
                        "$.role"
                ).value("ADMIN"))
                .andExpect(jsonPath(
                        "$.actif"
                ).value(false))
                .andExpect(jsonPath(
                        "$.motDePasseHash"
                ).doesNotExist());

        verify(compteUtilisateurService).update(
                any(Long.class),
                any(CompteUtilisateurUpdateRequest.class)
        );
    }

    @Test
    void shouldDeleteUserAccount()
            throws Exception {

        doNothing()
                .when(compteUtilisateurService)
                .delete(10L);

        mockMvc.perform(delete(
                        "/comptes-utilisateurs/{idUtilisateur}",
                        10L
                ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(compteUtilisateurService).delete(10L);
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist()
            throws Exception {

        when(compteUtilisateurService.findById(10L))
                .thenThrow(new ResourceNotFoundException(
                        "Le compte utilisateur ayant l'identifiant "
                                + "10 est introuvable."
                ));

        mockMvc.perform(get(
                        "/comptes-utilisateurs/{idUtilisateur}",
                        10L
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
                        "Le compte utilisateur ayant l'identifiant "
                                + "10 est introuvable."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/comptes-utilisateurs/10"));
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists()
            throws Exception {

        when(compteUtilisateurService.create(
                any(CompteUtilisateurCreateRequest.class)
        )).thenThrow(new DuplicateResourceException(
                "L'adresse électronique de connexion "
                        + "claire.dupont@example.com "
                        + "est déjà utilisée."
        ));

        mockMvc.perform(post(
                        "/comptes-utilisateurs"
                )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                objectMapper.writeValueAsString(
                                        createRequest
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
                        "L'adresse électronique de connexion "
                                + "claire.dupont@example.com "
                                + "est déjà utilisée."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value("/comptes-utilisateurs"));
    }

    @Test
    void shouldReturnBadRequestWhenCreateRequestIsInvalid()
            throws Exception {

        CompteUtilisateurCreateRequest invalidRequest =
                new CompteUtilisateurCreateRequest(
                        null,
                        "adresse-invalide",
                        "court",
                        null
                );

        mockMvc.perform(post(
                        "/comptes-utilisateurs"
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
                        "$.validationErrors.idPersonne"
                ).value(
                        "L'identifiant de la personne est obligatoire."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.emailConnexion"
                ).value(
                        "L'adresse électronique de connexion doit être valide."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.motDePasse"
                ).value(
                        "Le mot de passe doit contenir entre 8 et 100 caractères."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.role"
                ).value(
                        "Le rôle est obligatoire."
                ));
    }

    @Test
    void shouldReturnBadRequestWhenUpdateRequestIsInvalid()
            throws Exception {

        CompteUtilisateurUpdateRequest invalidRequest =
                new CompteUtilisateurUpdateRequest(
                        "",
                        "court",
                        null,
                        null
                );

        mockMvc.perform(put(
                        "/comptes-utilisateurs/{idUtilisateur}",
                        10L
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
                        "$.validationErrors.emailConnexion"
                ).value(
                        "L'adresse électronique de connexion est obligatoire."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.nouveauMotDePasse"
                ).value(
                        "Le nouveau mot de passe doit contenir entre 8 et 100 caractères."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.role"
                ).value(
                        "Le rôle est obligatoire."
                ))
                .andExpect(jsonPath(
                        "$.validationErrors.actif"
                ).value(
                        "L'état actif du compte est obligatoire."
                ));
    }
}
