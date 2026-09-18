package fr.afpa.backend.config;

import fr.afpa.backend.security.CompteUtilisateurDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationProvider authenticationProvider(
            CompteUtilisateurDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationConverter
    jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName(
                "roles"
        );

        authoritiesConverter.setAuthorityPrefix(
                "ROLE_"
        );

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter
                .setJwtGrantedAuthoritiesConverter(
                        authoritiesConverter
                );

        return authenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authenticationProvider(
                        authenticationProvider
                )
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/auth/login",
                                        "/auth/register"
                                )
                                .permitAll()
                                .requestMatchers(
                                        "/comptes-utilisateurs/**"
                                )
                                .hasRole("ADMIN")
                                .requestMatchers("/enseignants/**", "/responsables/**", "/responsabilites-legales/**", "/inscriptions/**")
                                .hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/matieres/**", "/periodes/**", "/eleves/**", "/scolarites/**", "/classes/**", "/enseignements/**", "/evaluations/**", "/notes/**", "/bulletins/**")
                                .hasAnyRole("ADMIN", "ENSEIGNANT", "RESPONSABLE")
                                .requestMatchers(HttpMethod.POST, "/evaluations", "/notes")
                                .hasAnyRole("ADMIN", "ENSEIGNANT")
                                .requestMatchers(HttpMethod.PUT, "/evaluations/**", "/notes/**")
                                .hasAnyRole("ADMIN", "ENSEIGNANT")
                                .requestMatchers(HttpMethod.DELETE, "/evaluations/**", "/notes/**")
                                .hasAnyRole("ADMIN", "ENSEIGNANT")
                                .requestMatchers("/eleves/**", "/classes/**", "/scolarites/**", "/enseignements/**", "/matieres/**", "/periodes/**")
                                .hasRole("ADMIN")
                                .anyRequest()
                                .denyAll()
                )
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                );

        return http.build();
    }
}
