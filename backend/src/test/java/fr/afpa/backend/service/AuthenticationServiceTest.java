package fr.afpa.backend.service;

import fr.afpa.backend.dto.authentication.AuthenticationResponse;
import fr.afpa.backend.dto.authentication.LoginRequest;
import fr.afpa.backend.dto.authentication.RegisterRequest;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurCreateRequest;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurResponse;
import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.exception.ForbiddenOperationException;
import fr.afpa.backend.exception.UnauthorizedException;
import fr.afpa.backend.security.CompteUtilisateurDetailsService;
import fr.afpa.backend.security.CompteUtilisateurPrincipal;
import fr.afpa.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager
            authenticationManager;

    @Mock
    private CompteUtilisateurService
            compteUtilisateurService;

    @Mock
    private CompteUtilisateurDetailsService
            compteUtilisateurDetailsService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService
            authenticationService;

    @Test
    void shouldLoginAndReturnJwt() {
        CompteUtilisateurPrincipal principal =
                createPrincipal(
                        RoleUtilisateur.ADMIN
                );

        Authentication authenticated =
                mock(Authentication.class);

        when(authenticated.getPrincipal())
                .thenReturn(principal);

        when(authenticationManager.authenticate(
                any(Authentication.class)
        )).thenReturn(authenticated);

        prepareJwt(principal);

        AuthenticationResponse response =
                authenticationService.login(
                        new LoginRequest(
                                "  ADMIN@EXAMPLE.COM ",
                                "MotDePasse123"
                        )
                );

        assertExpectedResponse(
                response,
                RoleUtilisateur.ADMIN
        );

        ArgumentCaptor<Authentication> captor =
                ArgumentCaptor.forClass(
                        Authentication.class
                );

        verify(authenticationManager)
                .authenticate(captor.capture());

        assertThat(captor.getValue().getName())
                .isEqualTo("admin@example.com");

        assertThat(captor.getValue().getCredentials())
                .isEqualTo("MotDePasse123");
    }

    @Test
    void shouldRejectInvalidCredentials() {
        when(authenticationManager.authenticate(
                any(Authentication.class)
        )).thenThrow(
                new BadCredentialsException(
                        "Identifiants invalides"
                )
        );

        assertThatThrownBy(() ->
                authenticationService.login(
                        new LoginRequest(
                                "admin@example.com",
                                "MotDePasse123"
                        )
                )
        )
                .isInstanceOf(
                        UnauthorizedException.class
                )
                .hasMessage(
                        "Les identifiants de connexion sont invalides."
                );

        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldRejectPublicRegistrationOnEmptyDatabase() {
        RegisterRequest request =
                createRegisterRequest(
                        RoleUtilisateur.ADMIN
                );

        assertThatThrownBy(() -> authenticationService.register(request, null))
                .isInstanceOf(UnauthorizedException.class);
        verify(compteUtilisateurService, never()).create(any());
    }

    @Test
    void shouldRejectFirstAccountWhenRoleIsNotAdmin() {
        RegisterRequest request =
                createRegisterRequest(
                        RoleUtilisateur.RESPONSABLE
                );

        assertThatThrownBy(() ->
                authenticationService.register(
                        request,
                        null
                )
        )
                .isInstanceOf(
                        UnauthorizedException.class
                )
                .hasMessage(
                        "Une authentification est nécessaire pour créer un compte utilisateur."
                );

        verify(
                compteUtilisateurService,
                never()
        ).create(any());
    }

    @Test
    void shouldRejectAdditionalAccountWithoutAuthentication() {
        RegisterRequest request =
                createRegisterRequest(
                        RoleUtilisateur.RESPONSABLE
                );


        assertThatThrownBy(() ->
                authenticationService.register(
                        request,
                        null
                )
        )
                .isInstanceOf(
                        UnauthorizedException.class
                )
                .hasMessage(
                        "Une authentification est nécessaire pour créer un compte utilisateur."
                );

        verify(
                compteUtilisateurService,
                never()
        ).create(any());
    }

    @Test
    void shouldRejectAdditionalAccountForNonAdmin() {
        RegisterRequest request =
                createRegisterRequest(
                        RoleUtilisateur.RESPONSABLE
                );

        Authentication authentication =
                createAuthentication(
                        "ROLE_RESPONSABLE"
                );


        assertThatThrownBy(() ->
                authenticationService.register(
                        request,
                        authentication
                )
        )
                .isInstanceOf(
                        ForbiddenOperationException.class
                );

        verify(
                compteUtilisateurService,
                never()
        ).create(any());
    }

    @Test
    void shouldAllowAdministratorToRegisterAdditionalAccount() {
        RegisterRequest request =
                createRegisterRequest(
                        RoleUtilisateur.RESPONSABLE
                );

        Authentication authentication =
                createAuthentication(
                        "ROLE_ADMIN"
                );

        CompteUtilisateurPrincipal principal =
                createPrincipal(
                        RoleUtilisateur.RESPONSABLE
                );


        prepareCreatedAccount(
                request,
                principal
        );

        prepareJwt(principal);

        AuthenticationResponse response =
                authenticationService.register(
                        request,
                        authentication
                );

        assertExpectedResponse(
                response,
                RoleUtilisateur.RESPONSABLE
        );

        verify(compteUtilisateurService)
                .create(any());
    }

    private RegisterRequest createRegisterRequest(
            RoleUtilisateur role
    ) {
        return new RegisterRequest(
                25L,
                "admin@example.com",
                "MotDePasse123",
                role
        );
    }

    private CompteUtilisateurPrincipal createPrincipal(
            RoleUtilisateur role
    ) {
        return new CompteUtilisateurPrincipal(
                12L,
                25L,
                "admin@example.com",
                "$2a$10$hash",
                role,
                true
        );
    }

    private Authentication createAuthentication(
            String authority
    ) {
        Authentication authentication =
                mock(Authentication.class);

        when(authentication.isAuthenticated())
                .thenReturn(true);

        doReturn(
                List.of(
                        new SimpleGrantedAuthority(
                                authority
                        )
                )
        )
                .when(authentication)
                .getAuthorities();

        return authentication;
    }

    private void prepareCreatedAccount(
            RegisterRequest request,
            CompteUtilisateurPrincipal principal
    ) {
        CompteUtilisateurResponse createdAccount =
                new CompteUtilisateurResponse(
                        principal.getIdUtilisateur(),
                        principal.getIdPersonne(),
                        "Dupont",
                        "Alice",
                        principal.getUsername(),
                        request.role(),
                        true,
                        OffsetDateTime.parse(
                                "2026-08-03T10:00:00+02:00"
                        )
                );

        when(compteUtilisateurService.create(
                any(CompteUtilisateurCreateRequest.class)
        )).thenReturn(createdAccount);

        when(compteUtilisateurDetailsService
                .loadUserByUsername(
                        createdAccount.emailConnexion()
                ))
                .thenReturn(principal);
    }

    private void prepareJwt(
            CompteUtilisateurPrincipal principal
    ) {
        when(jwtService.generateToken(principal))
                .thenReturn("jwt-token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(3600L);
    }

    private void assertExpectedResponse(
            AuthenticationResponse response,
            RoleUtilisateur expectedRole
    ) {
        assertThat(response.token())
                .isEqualTo("jwt-token");

        assertThat(response.tokenType())
                .isEqualTo("Bearer");

        assertThat(response.expiresIn())
                .isEqualTo(3600L);

        assertThat(response.idUtilisateur())
                .isEqualTo(12L);

        assertThat(response.idPersonne())
                .isEqualTo(25L);

        assertThat(response.emailConnexion())
                .isEqualTo("admin@example.com");

        assertThat(response.role())
                .isEqualTo(expectedRole);
    }
}
