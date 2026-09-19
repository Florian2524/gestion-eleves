package fr.afpa.backend.security;

import fr.afpa.backend.entity.CompteUtilisateur;
import fr.afpa.backend.entity.Personne;
import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.repository.CompteUtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveAccountJwtValidatorTest {
    private final CompteUtilisateurRepository accounts = mock(CompteUtilisateurRepository.class);
    private final ActiveAccountJwtValidator validator = new ActiveAccountJwtValidator(accounts);

    private Jwt jwt() {
        return Jwt.withTokenValue("test")
                .header("alg", "HS256")
                .subject("alice@example.com")
                .claim("idUtilisateur", 7L)
                .claim("idPersonne", 3L)
                .claim("roles", List.of("RESPONSABLE"))
                .build();
    }

    private CompteUtilisateur account(boolean active, RoleUtilisateur role) {
        CompteUtilisateur account = mock(CompteUtilisateur.class);
        Personne person = mock(Personne.class);
        when(account.isActif()).thenReturn(active);
        if (active) {
            when(account.getEmailConnexion()).thenReturn("alice@example.com");
            when(account.getPersonne()).thenReturn(person);
            when(person.getIdPersonne()).thenReturn(3L);
            when(account.getRole()).thenReturn(role);
        }
        return account;
    }

    @Test
    void acceptsCurrentActiveAccount() {
        CompteUtilisateur account = account(true, RoleUtilisateur.RESPONSABLE);
        when(accounts.findWithPersonneByIdUtilisateur(7L))
                .thenReturn(Optional.of(account));
        assertThat(validator.validate(jwt()).hasErrors()).isFalse();
    }

    @Test
    void rejectsDeactivatedAccount() {
        CompteUtilisateur account = account(false, RoleUtilisateur.RESPONSABLE);
        when(accounts.findWithPersonneByIdUtilisateur(7L))
                .thenReturn(Optional.of(account));
        assertThat(validator.validate(jwt()).hasErrors()).isTrue();
    }

    @Test
    void rejectsDeletedAccount() {
        when(accounts.findWithPersonneByIdUtilisateur(7L)).thenReturn(Optional.empty());
        assertThat(validator.validate(jwt()).hasErrors()).isTrue();
    }

    @Test
    void rejectsStaleRole() {
        CompteUtilisateur account = account(true, RoleUtilisateur.ADMIN);
        when(accounts.findWithPersonneByIdUtilisateur(7L))
                .thenReturn(Optional.of(account));
        assertThat(validator.validate(jwt()).hasErrors()).isTrue();
    }
}
