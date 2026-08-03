package fr.afpa.backend.security;

import fr.afpa.backend.entity.CompteUtilisateur;
import fr.afpa.backend.entity.Personne;
import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.repository.CompteUtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompteUtilisateurDetailsServiceTest {

    @Mock
    private CompteUtilisateurRepository
            compteUtilisateurRepository;

    @InjectMocks
    private CompteUtilisateurDetailsService
            compteUtilisateurDetailsService;

    @Test
    void shouldNormalizeEmailAndMapAccountToPrincipal() {
        CompteUtilisateur compteUtilisateur =
                createAccount(true);

        when(compteUtilisateurRepository
                .findByEmailConnexion(
                        "responsable@example.com"
                ))
                .thenReturn(
                        Optional.of(compteUtilisateur)
                );

        CompteUtilisateurPrincipal principal =
                (CompteUtilisateurPrincipal)
                        compteUtilisateurDetailsService
                                .loadUserByUsername(
                                        "  RESPONSABLE@EXAMPLE.COM  "
                                );

        assertThat(principal.getIdUtilisateur())
                .isEqualTo(12L);

        assertThat(principal.getIdPersonne())
                .isEqualTo(25L);

        assertThat(principal.getUsername())
                .isEqualTo(
                        "responsable@example.com"
                );

        assertThat(principal.getPassword())
                .isEqualTo("$2a$10$hash");

        assertThat(principal.getRole())
                .isEqualTo(
                        RoleUtilisateur.RESPONSABLE
                );

        assertThat(principal.isEnabled())
                .isTrue();

        assertThat(principal.getAuthorities())
                .extracting(
                        GrantedAuthority::getAuthority
                )
                .containsExactly(
                        "ROLE_RESPONSABLE"
                );

        verify(compteUtilisateurRepository)
                .findByEmailConnexion(
                        "responsable@example.com"
                );
    }

    @Test
    void shouldRejectUnknownAccount() {
        when(compteUtilisateurRepository
                .findByEmailConnexion(
                        "inconnu@example.com"
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compteUtilisateurDetailsService
                        .loadUserByUsername(
                                "INCONNU@example.com"
                        )
        )
                .isInstanceOf(
                        UsernameNotFoundException.class
                )
                .hasMessage(
                        "Le compte utilisateur est introuvable."
                );
    }

    @Test
    void shouldDisablePrincipalWhenAccountIsInactive() {
        CompteUtilisateur compteUtilisateur =
                createAccount(false);

        when(compteUtilisateurRepository
                .findByEmailConnexion(
                        "responsable@example.com"
                ))
                .thenReturn(
                        Optional.of(compteUtilisateur)
                );

        CompteUtilisateurPrincipal principal =
                (CompteUtilisateurPrincipal)
                        compteUtilisateurDetailsService
                                .loadUserByUsername(
                                        "responsable@example.com"
                                );

        assertThat(principal.isEnabled())
                .isFalse();
    }

    private CompteUtilisateur createAccount(
            boolean actif
    ) {
        Personne personne = mock(Personne.class);

        CompteUtilisateur compteUtilisateur =
                mock(CompteUtilisateur.class);

        when(personne.getIdPersonne())
                .thenReturn(25L);

        when(compteUtilisateur.getIdUtilisateur())
                .thenReturn(12L);

        when(compteUtilisateur.getPersonne())
                .thenReturn(personne);

        when(compteUtilisateur.getEmailConnexion())
                .thenReturn(
                        "responsable@example.com"
                );

        when(compteUtilisateur.getMotDePasseHash())
                .thenReturn("$2a$10$hash");

        when(compteUtilisateur.getRole())
                .thenReturn(
                        RoleUtilisateur.RESPONSABLE
                );

        when(compteUtilisateur.isActif())
                .thenReturn(actif);

        return compteUtilisateur;
    }
}
