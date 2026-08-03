package fr.afpa.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.afpa.backend.dto.authentication.AuthenticationResponse;
import fr.afpa.backend.dto.authentication.LoginRequest;
import fr.afpa.backend.dto.authentication.RegisterRequest;
import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.exception.ForbiddenOperationException;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.UnauthorizedException;
import fr.afpa.backend.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService
            authenticationService;

    @Test
    void shouldLogin() throws Exception {
        when(authenticationService.login(
                any(LoginRequest.class)
        )).thenReturn(authenticationResponse());

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginRequest(
                                                        "admin@example.com",
                                                        "MotDePasse123"
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.token")
                                .value("jwt-token")
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.expiresIn")
                                .value(3600)
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("ADMIN")
                );
    }

    @Test
    void shouldRegisterAccount() throws Exception {
        when(authenticationService.register(
                any(RegisterRequest.class),
                nullable(Authentication.class)
        )).thenReturn(authenticationResponse());

        RegisterRequest request =
                new RegisterRequest(
                        25L,
                        "admin@example.com",
                        "MotDePasse123",
                        RoleUtilisateur.ADMIN
                );

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                "/comptes-utilisateurs/12"
                        )
                )
                .andExpect(
                        jsonPath("$.idUtilisateur")
                                .value(12)
                );
    }

    @Test
    void shouldRejectInvalidLoginData() throws Exception {
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "emailConnexion": "adresse-invalide",
                                          "motDePasse": "court"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                );
    }

    @Test
    void shouldRejectInvalidRegisterData() throws Exception {
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "idPersonne": null,
                                          "emailConnexion": "invalide",
                                          "motDePasse": "court",
                                          "role": null
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                );
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginFails()
            throws Exception {

        when(authenticationService.login(
                any(LoginRequest.class)
        )).thenThrow(
                new UnauthorizedException(
                        "Les identifiants de connexion sont invalides."
                )
        );

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "emailConnexion": "admin@example.com",
                                          "motDePasse": "MotDePasse123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Les identifiants de connexion sont invalides."
                                )
                );
    }

    @Test
    void shouldReturnForbiddenWhenRegistrationIsDenied()
            throws Exception {

        when(authenticationService.register(
                any(RegisterRequest.class),
                nullable(Authentication.class)
        )).thenThrow(
                new ForbiddenOperationException(
                        "Seul un administrateur peut créer un nouveau compte utilisateur."
                )
        );

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "idPersonne": 25,
                                          "emailConnexion": "responsable@example.com",
                                          "motDePasse": "MotDePasse123",
                                          "role": "RESPONSABLE"
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                );
    }

    private AuthenticationResponse
    authenticationResponse() {

        return new AuthenticationResponse(
                "jwt-token",
                "Bearer",
                3600L,
                12L,
                25L,
                "admin@example.com",
                RoleUtilisateur.ADMIN
        );
    }
}
