package fr.afpa.backend.service;

import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurCreateRequest;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurResponse;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurUpdateRequest;
import fr.afpa.backend.entity.CompteUtilisateur;
import fr.afpa.backend.entity.Personne;
import fr.afpa.backend.entity.Enseignant;
import fr.afpa.backend.entity.Responsable;
import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.exception.DuplicateResourceException;
import fr.afpa.backend.exception.InvalidAccountRoleException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.mapper.CompteUtilisateurMapper;
import fr.afpa.backend.repository.CompteUtilisateurRepository;
import fr.afpa.backend.repository.PersonneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompteUtilisateurServiceTest {

    @Mock
    private CompteUtilisateurRepository
            compteUtilisateurRepository;

    @Mock
    private PersonneRepository personneRepository;

    @Mock
    private CompteUtilisateurMapper
            compteUtilisateurMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Responsable personne;

    @Mock
    private CompteUtilisateur compteUtilisateur;

    private CompteUtilisateurService
            compteUtilisateurService;

    private CompteUtilisateurCreateRequest createRequest;
    private CompteUtilisateurUpdateRequest updateRequest;
    private CompteUtilisateurResponse response;

    @BeforeEach
    void setUp() {
        compteUtilisateurService =
                new CompteUtilisateurService(
                        compteUtilisateurRepository,
                        personneRepository,
                        compteUtilisateurMapper,
                        passwordEncoder
                );

        createRequest =
                new CompteUtilisateurCreateRequest(
                        1L,
                        "  CLAIRE.DUPONT@EXAMPLE.COM  ",
                        "MotDePasse123",
                        RoleUtilisateur.RESPONSABLE
                );

        updateRequest =
                new CompteUtilisateurUpdateRequest(
                        "  CLAIRE.DUPONT@ECOLE.EXAMPLE.COM  ",
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
    void shouldReturnAllUserAccounts() {
        when(compteUtilisateurRepository.findAll())
                .thenReturn(List.of(compteUtilisateur));

        when(compteUtilisateurMapper.toResponse(
                compteUtilisateur
        )).thenReturn(response);

        List<CompteUtilisateurResponse> result =
                compteUtilisateurService.findAll();

        assertThat(result)
                .containsExactly(response);

        verify(compteUtilisateurRepository).findAll();

        verify(compteUtilisateurMapper).toResponse(
                compteUtilisateur
        );
    }

    @Test
    void shouldReturnUserAccountById() {
        when(compteUtilisateurRepository.findById(10L))
                .thenReturn(Optional.of(compteUtilisateur));

        when(compteUtilisateurMapper.toResponse(
                compteUtilisateur
        )).thenReturn(response);

        CompteUtilisateurResponse result =
                compteUtilisateurService.findById(10L);

        assertThat(result).isEqualTo(response);

        verify(compteUtilisateurRepository).findById(10L);

        verify(compteUtilisateurMapper).toResponse(
                compteUtilisateur
        );
    }

    @Test
    void shouldRejectUnknownUserAccount() {
        when(compteUtilisateurRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compteUtilisateurService.findById(10L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Le compte utilisateur ayant l'identifiant "
                                + "10 est introuvable."
                );

        verify(compteUtilisateurRepository).findById(10L);
        verifyNoInteractions(compteUtilisateurMapper);
    }

    @Test
    void shouldCreateUserAccountAndEncodePassword() {
        String normalizedEmail =
                "claire.dupont@example.com";

        String passwordHash =
                "$2a$10$motDePasseEncode";

        when(personneRepository.findById(1L))
                .thenReturn(Optional.of(personne));

        when(compteUtilisateurRepository
                .existsByPersonneIdPersonne(1L))
                .thenReturn(false);

        when(compteUtilisateurRepository
                .existsByEmailConnexion(normalizedEmail))
                .thenReturn(false);

        when(passwordEncoder.encode("MotDePasse123"))
                .thenReturn(passwordHash);

        when(compteUtilisateurMapper.toEntity(
                personne,
                normalizedEmail,
                passwordHash,
                RoleUtilisateur.RESPONSABLE
        )).thenReturn(compteUtilisateur);

        when(compteUtilisateurRepository.save(
                compteUtilisateur
        )).thenReturn(compteUtilisateur);

        when(compteUtilisateurMapper.toResponse(
                compteUtilisateur
        )).thenReturn(response);

        CompteUtilisateurResponse result =
                compteUtilisateurService.create(
                        createRequest
                );

        assertThat(result).isEqualTo(response);

        verify(personneRepository).findById(1L);

        verify(compteUtilisateurRepository)
                .existsByPersonneIdPersonne(1L);

        verify(compteUtilisateurRepository)
                .existsByEmailConnexion(normalizedEmail);

        verify(passwordEncoder).encode(
                "MotDePasse123"
        );

        verify(compteUtilisateurMapper).toEntity(
                personne,
                normalizedEmail,
                passwordHash,
                RoleUtilisateur.RESPONSABLE
        );

        verify(compteUtilisateurRepository).save(
                compteUtilisateur
        );
    }

    @Test
    void shouldRejectCreationWhenPersonDoesNotExist() {
        when(personneRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compteUtilisateurService.create(
                        createRequest
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "La personne ayant l'identifiant "
                                + "1 est introuvable."
                );

        verify(personneRepository).findById(1L);

        verifyNoInteractions(
                compteUtilisateurRepository,
                compteUtilisateurMapper,
                passwordEncoder
        );
    }

    @Test
    void shouldRejectTeacherRoleForNonTeacher() {
        when(personneRepository.findById(1L)).thenReturn(Optional.of(personne));
        var request = new CompteUtilisateurCreateRequest(1L, "teacher@example.com", "MotDePasse123", RoleUtilisateur.ENSEIGNANT);

        assertThatThrownBy(() -> compteUtilisateurService.create(request))
                .isInstanceOf(InvalidAccountRoleException.class);
        verifyNoInteractions(passwordEncoder, compteUtilisateurMapper);
        verify(compteUtilisateurRepository, never()).save(any(CompteUtilisateur.class));
    }

    @Test
    void shouldAllowAdminRoleForPlainPerson() {
        Personne administrator = new Personne("Martin", "Alice", null, null, null);
        var request = new CompteUtilisateurCreateRequest(1L, "admin@example.com", "MotDePasse123", RoleUtilisateur.ADMIN);
        when(personneRepository.findById(1L)).thenReturn(Optional.of(administrator));
        when(passwordEncoder.encode("MotDePasse123")).thenReturn("encoded");
        when(compteUtilisateurMapper.toEntity(administrator, "admin@example.com", "encoded", RoleUtilisateur.ADMIN))
                .thenReturn(compteUtilisateur);
        when(compteUtilisateurRepository.save(compteUtilisateur)).thenReturn(compteUtilisateur);
        when(compteUtilisateurMapper.toResponse(compteUtilisateur)).thenReturn(response);

        assertThat(compteUtilisateurService.create(request)).isEqualTo(response);
        verify(compteUtilisateurRepository).save(compteUtilisateur);
    }

    @Test
    void shouldCreateTeacherAccountForTeacher() {
        Personne teacher = new Enseignant("Dupont", "Jean", null, null, null, "ENS-1");
        var request = new CompteUtilisateurCreateRequest(1L, "teacher@example.com", "MotDePasse123", RoleUtilisateur.ENSEIGNANT);
        when(personneRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(passwordEncoder.encode("MotDePasse123")).thenReturn("encoded");
        when(compteUtilisateurMapper.toEntity(teacher, "teacher@example.com", "encoded", RoleUtilisateur.ENSEIGNANT))
                .thenReturn(compteUtilisateur);
        when(compteUtilisateurRepository.save(compteUtilisateur)).thenReturn(compteUtilisateur);
        when(compteUtilisateurMapper.toResponse(compteUtilisateur)).thenReturn(response);

        assertThat(compteUtilisateurService.create(request)).isEqualTo(response);
        verify(compteUtilisateurRepository).save(compteUtilisateur);
    }

    @Test
    void shouldRejectGuardianRoleForNonGuardian() {
        Personne teacher = new Enseignant("Dupont", "Jean", null, null, null, "ENS-1");
        when(personneRepository.findById(1L)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> compteUtilisateurService.create(createRequest))
                .isInstanceOf(InvalidAccountRoleException.class);
        verifyNoInteractions(passwordEncoder, compteUtilisateurMapper);
    }

    @Test
    void shouldRejectIncompatibleRoleChange() {
        when(compteUtilisateurRepository.findById(10L)).thenReturn(Optional.of(compteUtilisateur));
        when(compteUtilisateur.getPersonne()).thenReturn(personne);
        var request = new CompteUtilisateurUpdateRequest("test@example.com", null, RoleUtilisateur.ENSEIGNANT, true);

        assertThatThrownBy(() -> compteUtilisateurService.update(10L, request))
                .isInstanceOf(InvalidAccountRoleException.class);
        verify(compteUtilisateurRepository, never()).save(any(CompteUtilisateur.class));
    }

    @Test
    void shouldRejectCreationWhenPersonAlreadyHasAccount() {
        when(personneRepository.findById(1L))
                .thenReturn(Optional.of(personne));

        when(compteUtilisateurRepository
                .existsByPersonneIdPersonne(1L))
                .thenReturn(true);

        assertThatThrownBy(() ->
                compteUtilisateurService.create(
                        createRequest
                )
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "La personne ayant l'identifiant "
                                + "1 possède déjà un compte utilisateur."
                );

        verify(personneRepository).findById(1L);

        verify(compteUtilisateurRepository)
                .existsByPersonneIdPersonne(1L);

        verify(compteUtilisateurRepository, never())
                .existsByEmailConnexion(anyString());

        verify(compteUtilisateurRepository, never())
                .save(any(CompteUtilisateur.class));

        verifyNoInteractions(
                compteUtilisateurMapper,
                passwordEncoder
        );
    }

    @Test
    void shouldRejectCreationWhenEmailAlreadyExists() {
        String normalizedEmail =
                "claire.dupont@example.com";

        when(personneRepository.findById(1L))
                .thenReturn(Optional.of(personne));

        when(compteUtilisateurRepository
                .existsByPersonneIdPersonne(1L))
                .thenReturn(false);

        when(compteUtilisateurRepository
                .existsByEmailConnexion(normalizedEmail))
                .thenReturn(true);

        assertThatThrownBy(() ->
                compteUtilisateurService.create(
                        createRequest
                )
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "L'adresse électronique de connexion "
                                + normalizedEmail
                                + " est déjà utilisée."
                );

        verify(compteUtilisateurRepository)
                .existsByEmailConnexion(normalizedEmail);

        verify(compteUtilisateurRepository, never())
                .save(any(CompteUtilisateur.class));

        verifyNoInteractions(
                compteUtilisateurMapper,
                passwordEncoder
        );
    }

    @Test
    void shouldUpdateUserAccountWithoutChangingPassword() {
        String normalizedEmail =
                "claire.dupont@ecole.example.com";

        CompteUtilisateurResponse updatedResponse =
                new CompteUtilisateurResponse(
                        10L,
                        1L,
                        "Dupont",
                        "Claire",
                        normalizedEmail,
                        RoleUtilisateur.ADMIN,
                        false,
                        response.dateCreation()
                );

        when(compteUtilisateurRepository.findById(10L))
                .thenReturn(Optional.of(compteUtilisateur));

        when(compteUtilisateurRepository
                .existsByEmailConnexionAndIdUtilisateurNot(
                        normalizedEmail,
                        10L
                ))
                .thenReturn(false);

        when(compteUtilisateurRepository.save(
                compteUtilisateur
        )).thenReturn(compteUtilisateur);

        when(compteUtilisateurMapper.toResponse(
                compteUtilisateur
        )).thenReturn(updatedResponse);

        CompteUtilisateurResponse result =
                compteUtilisateurService.update(
                        10L,
                        updateRequest
                );

        assertThat(result).isEqualTo(updatedResponse);

        verify(compteUtilisateurMapper).updateEntity(
                compteUtilisateur,
                normalizedEmail,
                RoleUtilisateur.ADMIN,
                false,
                null
        );

        verifyNoInteractions(passwordEncoder);

        verify(compteUtilisateurRepository).save(
                compteUtilisateur
        );
    }

    @Test
    void shouldUpdateUserAccountAndEncodeNewPassword() {
        String normalizedEmail =
                "claire.dupont@ecole.example.com";

        String passwordHash =
                "$2a$10$nouveauMotDePasseEncode";

        CompteUtilisateurUpdateRequest requestWithPassword =
                new CompteUtilisateurUpdateRequest(
                        normalizedEmail,
                        "NouveauMotDePasse123",
                        RoleUtilisateur.ADMIN,
                        true
                );

        when(compteUtilisateurRepository.findById(10L))
                .thenReturn(Optional.of(compteUtilisateur));

        when(compteUtilisateurRepository
                .existsByEmailConnexionAndIdUtilisateurNot(
                        normalizedEmail,
                        10L
                ))
                .thenReturn(false);

        when(passwordEncoder.encode(
                "NouveauMotDePasse123"
        )).thenReturn(passwordHash);

        when(compteUtilisateurRepository.save(
                compteUtilisateur
        )).thenReturn(compteUtilisateur);

        when(compteUtilisateurMapper.toResponse(
                compteUtilisateur
        )).thenReturn(response);

        CompteUtilisateurResponse result =
                compteUtilisateurService.update(
                        10L,
                        requestWithPassword
                );

        assertThat(result).isEqualTo(response);

        verify(passwordEncoder).encode(
                "NouveauMotDePasse123"
        );

        verify(compteUtilisateurMapper).updateEntity(
                compteUtilisateur,
                normalizedEmail,
                RoleUtilisateur.ADMIN,
                true,
                passwordHash
        );
    }

    @Test
    void shouldRejectUpdateWhenEmailAlreadyExists() {
        String normalizedEmail =
                "claire.dupont@ecole.example.com";

        when(compteUtilisateurRepository.findById(10L))
                .thenReturn(Optional.of(compteUtilisateur));

        when(compteUtilisateurRepository
                .existsByEmailConnexionAndIdUtilisateurNot(
                        normalizedEmail,
                        10L
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                compteUtilisateurService.update(
                        10L,
                        updateRequest
                )
        )
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(
                        "L'adresse électronique de connexion "
                                + normalizedEmail
                                + " est déjà utilisée."
                );

        verify(compteUtilisateurRepository, never())
                .save(any(CompteUtilisateur.class));

        verifyNoInteractions(
                compteUtilisateurMapper,
                passwordEncoder
        );
    }

    @Test
    void shouldRejectUpdateWhenAccountDoesNotExist() {
        when(compteUtilisateurRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compteUtilisateurService.update(
                        10L,
                        updateRequest
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Le compte utilisateur ayant l'identifiant "
                                + "10 est introuvable."
                );

        verify(compteUtilisateurRepository).findById(10L);

        verifyNoInteractions(
                compteUtilisateurMapper,
                passwordEncoder
        );
    }

    @Test
    void shouldDeleteUserAccount() {
        when(compteUtilisateurRepository.findById(10L))
                .thenReturn(Optional.of(compteUtilisateur));

        compteUtilisateurService.delete(10L);

        verify(compteUtilisateurRepository).findById(10L);

        verify(compteUtilisateurRepository).delete(
                compteUtilisateur
        );
    }

    @Test
    void shouldRejectDeletionWhenAccountDoesNotExist() {
        when(compteUtilisateurRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                compteUtilisateurService.delete(10L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Le compte utilisateur ayant l'identifiant "
                                + "10 est introuvable."
                );

        verify(compteUtilisateurRepository).findById(10L);

        verify(compteUtilisateurRepository, never())
                .delete(any(CompteUtilisateur.class));
    }
}
