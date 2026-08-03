package fr.afpa.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectProtectedRequestWithoutToken()
            throws Exception {

        mockMvc.perform(get("/matieres"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedUserOnProtectedEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/matieres")
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_RESPONSABLE"
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectNonAdminOnUserAccountEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/comptes-utilisateurs")
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_RESPONSABLE"
                                                )
                                        )
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminOnUserAccountEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/comptes-utilisateurs")
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_ADMIN"
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldKeepLoginEndpointPublic()
            throws Exception {

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }
}
