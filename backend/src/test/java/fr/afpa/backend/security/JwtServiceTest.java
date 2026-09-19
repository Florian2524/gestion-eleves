package fr.afpa.backend.security;

import fr.afpa.backend.config.JwtConfig;
import fr.afpa.backend.config.SecurityConfig;
import fr.afpa.backend.entity.RoleUtilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();

        String encodedSecret = Base64
                .getEncoder()
                .encodeToString(
                        "gestion-eleves-test-secret-key-2026"
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

        SecretKey secretKey =
                jwtConfig.jwtSecretKey(encodedSecret);

        JwtEncoder jwtEncoder =
                jwtConfig.jwtEncoder(secretKey);

        ActiveAccountJwtValidator accountValidator = mock(ActiveAccountJwtValidator.class);
        when(accountValidator.validate(any(Jwt.class))).thenReturn(OAuth2TokenValidatorResult.success());
        jwtDecoder = jwtConfig.jwtDecoder(secretKey, accountValidator);

        jwtService = new JwtService(
                jwtEncoder,
                3600L
        );
    }

    @Test
    void shouldGenerateSignedJwtWithExpectedClaims() {
        CompteUtilisateurPrincipal principal =
                new CompteUtilisateurPrincipal(
                        7L,
                        19L,
                        "admin@example.com",
                        "$2a$10$hash",
                        RoleUtilisateur.ADMIN,
                        true
                );

        String token =
                jwtService.generateToken(principal);

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject())
                .isEqualTo("admin@example.com");

        assertThat(jwt.getClaimAsString("iss"))
                .isEqualTo("gestion-eleves-api");

        assertThat(jwt.getClaimAsStringList("roles"))
                .containsExactly("ADMIN");

        assertThat(
                ((Number) jwt.getClaim(
                        "idUtilisateur"
                )).longValue()
        ).isEqualTo(7L);

        assertThat(
                ((Number) jwt.getClaim(
                        "idPersonne"
                )).longValue()
        ).isEqualTo(19L);

        assertThat(jwt.getIssuedAt())
                .isNotNull();

        assertThat(jwt.getExpiresAt())
                .isNotNull();

        assertThat(
                Duration.between(
                        jwt.getIssuedAt(),
                        jwt.getExpiresAt()
                ).getSeconds()
        ).isEqualTo(3600L);
    }

    @Test
    void shouldExposeConfiguredExpiration() {
        assertThat(
                jwtService.getExpirationSeconds()
        ).isEqualTo(3600L);
    }

    @Test
    void shouldConvertRolesClaimToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("admin@example.com")
                .claim(
                        "roles",
                        List.of(
                                "ADMIN",
                                "ENSEIGNANT"
                        )
                )
                .build();

        var authentication =
                new SecurityConfig()
                        .jwtAuthenticationConverter()
                        .convert(jwt);

        assertThat(authentication)
                .isNotNull();

        assertThat(authentication.getAuthorities())
                .extracting(
                        GrantedAuthority::getAuthority
                )
                .containsExactlyInAnyOrder(
                        "ROLE_ADMIN",
                        "ROLE_ENSEIGNANT"
                );
    }
}
