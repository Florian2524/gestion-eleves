package fr.afpa.backend.security;

import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.repository.EnseignementRepository;
import fr.afpa.backend.repository.EvaluationRepository;
import fr.afpa.backend.repository.NoteRepository;
import fr.afpa.backend.repository.EleveRepository;
import fr.afpa.backend.repository.ScolariteRepository;
import fr.afpa.backend.repository.ResponsabiliteLegaleRepository;
import fr.afpa.backend.repository.ClasseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityExpressionsTest {

    private static final Long ID_PERSONNE = 10L;

    @Mock
    private EnseignementRepository enseignementRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private NoteRepository noteRepository;
    @Mock private EleveRepository eleveRepository;
    @Mock private ScolariteRepository scolariteRepository;
    @Mock private ResponsabiliteLegaleRepository responsabiliteLegaleRepository;
    @Mock private ClasseRepository classeRepository;

    private SecurityExpressions securityExpressions;

    @BeforeEach
    void setUp() {
        securityExpressions = new SecurityExpressions(
                enseignementRepository,
                evaluationRepository,
                noteRepository,
                eleveRepository,
                scolariteRepository,
                responsabiliteLegaleRepository,
                classeRepository
        );
    }

    @Test
    void estProprietaireEnseignementRetourneTruePourSonEnseignement() {
        Authentication authentication =
                createAuthentication(RoleUtilisateur.ENSEIGNANT);

        when(
                enseignementRepository
                        .existsByIdEnseignementAndEnseignant_IdPersonne(
                                20L,
                                ID_PERSONNE
                        )
        ).thenReturn(true);

        boolean result =
                securityExpressions.estProprietaireEnseignement(
                        20L,
                        authentication
                );

        assertThat(result).isTrue();
    }

    @Test
    void estProprietaireEnseignementRetourneFalsePourUnAutreEnseignant() {
        Authentication authentication =
                createAuthentication(RoleUtilisateur.ENSEIGNANT);

        when(
                enseignementRepository
                        .existsByIdEnseignementAndEnseignant_IdPersonne(
                                20L,
                                ID_PERSONNE
                        )
        ).thenReturn(false);

        boolean result =
                securityExpressions.estProprietaireEnseignement(
                        20L,
                        authentication
                );

        assertThat(result).isFalse();
    }

    @Test
    void estProprietaireEvaluationRetourneTruePourSonEvaluation() {
        Authentication authentication =
                createAuthentication(RoleUtilisateur.ENSEIGNANT);

        when(
                evaluationRepository
                        .existsByIdEvaluationAndEnseignement_Enseignant_IdPersonne(
                                30L,
                                ID_PERSONNE
                        )
        ).thenReturn(true);

        boolean result =
                securityExpressions.estProprietaireEvaluation(
                        30L,
                        authentication
                );

        assertThat(result).isTrue();
    }

    @Test
    void estProprietaireNoteRetourneTruePourSaNote() {
        Authentication authentication =
                createAuthentication(RoleUtilisateur.ENSEIGNANT);

        when(
                noteRepository
                        .existsByIdNoteAndEvaluation_Enseignement_Enseignant_IdPersonne(
                                40L,
                                ID_PERSONNE
                        )
        ).thenReturn(true);

        boolean result =
                securityExpressions.estProprietaireNote(
                        40L,
                        authentication
                );

        assertThat(result).isTrue();
    }

    @Test
    void refuseUnAdministrateurDansUneExpressionReserveeAuxEnseignants() {
        Authentication authentication =
                createAuthentication(RoleUtilisateur.ADMIN);

        boolean result =
                securityExpressions.estProprietaireNote(
                        40L,
                        authentication
                );

        assertThat(result).isFalse();

        verify(
                noteRepository,
                never()
        ).existsByIdNoteAndEvaluation_Enseignement_Enseignant_IdPersonne(
                40L,
                ID_PERSONNE
        );
    }

    @Test
    void refuseUnResponsableDansUneExpressionReserveeAuxEnseignants() {
        Authentication authentication =
                createAuthentication(RoleUtilisateur.RESPONSABLE);

        boolean result =
                securityExpressions.estProprietaireEvaluation(
                        30L,
                        authentication
                );

        assertThat(result).isFalse();

        verify(
                evaluationRepository,
                never()
        ).existsByIdEvaluationAndEnseignement_Enseignant_IdPersonne(
                30L,
                ID_PERSONNE
        );
    }

    @Test
    void refuseUneAuthentificationAbsente() {
        boolean result =
                securityExpressions.estProprietaireEnseignement(
                        20L,
                        null
                );

        assertThat(result).isFalse();

        verify(
                enseignementRepository,
                never()
        ).existsByIdEnseignementAndEnseignant_IdPersonne(
                20L,
                ID_PERSONNE
        );
    }

    @Test
    void refuseUnIdentifiantNull() {
        Authentication authentication =
                createAuthentication(RoleUtilisateur.ENSEIGNANT);

        boolean result =
                securityExpressions.estProprietaireNote(
                        null,
                        authentication
                );

        assertThat(result).isFalse();
    }

    private Authentication createAuthentication(
            RoleUtilisateur role
    ) {
        CompteUtilisateurPrincipal principal =
                new CompteUtilisateurPrincipal(
                        1L,
                        ID_PERSONNE,
                        "utilisateur@test.fr",
                        "mot-de-passe-hache",
                        role,
                        true
                );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    private Authentication jwtAuthentication(RoleUtilisateur role) {
        Jwt jwt = Jwt.withTokenValue("test")
                .header("alg", "none")
                .claim("sub", "test@example.fr")
                .claim("idPersonne", ID_PERSONNE)
                .build();
        return new JwtAuthenticationToken(jwt,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    @Test
    void jwtEnseignantEstReconnuPourSonEvaluation() {
        when(evaluationRepository.existsByIdEvaluationAndEnseignement_Enseignant_IdPersonne(30L, ID_PERSONNE))
                .thenReturn(true);
        assertThat(securityExpressions.estProprietaireEvaluation(30L,
                jwtAuthentication(RoleUtilisateur.ENSEIGNANT))).isTrue();
    }

    @Test
    void responsableNeVoitQueSonEleve() {
        when(responsabiliteLegaleRepository.existsByResponsable_IdPersonneAndEleve_IdPersonne(ID_PERSONNE, 1L))
                .thenReturn(true);
        Authentication authentication = jwtAuthentication(RoleUtilisateur.RESPONSABLE);
        assertThat(securityExpressions.peutConsulterEleve(1L, authentication)).isTrue();
        assertThat(securityExpressions.peutConsulterEleve(2L, authentication)).isFalse();
    }

    @Test
    void responsableNeVoitQueLaNoteEtLaScolariteDeSonEleve() {
        when(noteRepository.estLieAuResponsable(11L, ID_PERSONNE)).thenReturn(true);
        when(scolariteRepository.estLieAuResponsable(21L, ID_PERSONNE)).thenReturn(true);
        Authentication authentication = jwtAuthentication(RoleUtilisateur.RESPONSABLE);
        assertThat(securityExpressions.peutConsulterNote(11L, authentication)).isTrue();
        assertThat(securityExpressions.peutConsulterNote(12L, authentication)).isFalse();
        assertThat(securityExpressions.peutConsulterScolarite(21L, authentication)).isTrue();
        assertThat(securityExpressions.peutConsulterScolarite(22L, authentication)).isFalse();
    }
}
