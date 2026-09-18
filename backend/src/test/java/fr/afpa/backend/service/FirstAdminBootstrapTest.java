package fr.afpa.backend.service;

import fr.afpa.backend.entity.CompteUtilisateur;
import fr.afpa.backend.entity.Personne;
import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.repository.CompteUtilisateurRepository;
import fr.afpa.backend.repository.PersonneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirstAdminBootstrapTest {
    @Mock CompteUtilisateurRepository comptes;
    @Mock PersonneRepository personnes;
    @Mock PasswordEncoder encoder;
    FirstAdminBootstrap bootstrap;

    @BeforeEach void setUp() {
        bootstrap = new FirstAdminBootstrap(comptes, personnes, encoder);
        ReflectionTestUtils.setField(bootstrap, "email", "ADMIN@example.com");
        ReflectionTestUtils.setField(bootstrap, "password", "temporary-secret");
        ReflectionTestUtils.setField(bootstrap, "nom", "Admin");
        ReflectionTestUtils.setField(bootstrap, "prenom", "Premier");
    }

    @Test void createsFirstAdminWithHashedPassword() {
        when(personnes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.encode("temporary-secret")).thenReturn("encoded-password");
        bootstrap.run(new DefaultApplicationArguments());
        verify(personnes).save(any(Personne.class));
        verify(comptes).save(org.mockito.ArgumentMatchers.argThat(account ->
                account.getRole() == RoleUtilisateur.ADMIN
                        && account.getMotDePasseHash().equals("encoded-password")
                        && account.getEmailConnexion().equals("admin@example.com")));
    }

    @Test void doesNotBootstrapWhenAnAccountAlreadyExists() {
        when(comptes.count()).thenReturn(1L);
        bootstrap.run(new DefaultApplicationArguments());
        verify(personnes, never()).save(any());
        verify(comptes, never()).save(any(CompteUtilisateur.class));
        verify(encoder, never()).encode(any());
    }
}
