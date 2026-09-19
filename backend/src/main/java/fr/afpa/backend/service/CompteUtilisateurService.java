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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CompteUtilisateurService {

    private final CompteUtilisateurRepository
            compteUtilisateurRepository;

    private final PersonneRepository personneRepository;
    private final CompteUtilisateurMapper compteUtilisateurMapper;
    private final PasswordEncoder passwordEncoder;

    public CompteUtilisateurService(
            CompteUtilisateurRepository compteUtilisateurRepository,
            PersonneRepository personneRepository,
            CompteUtilisateurMapper compteUtilisateurMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.compteUtilisateurRepository =
                compteUtilisateurRepository;

        this.personneRepository = personneRepository;
        this.compteUtilisateurMapper = compteUtilisateurMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<CompteUtilisateurResponse> findAll() {
        return compteUtilisateurRepository.findAll()
                .stream()
                .map(compteUtilisateurMapper::toResponse)
                .toList();
    }

    public CompteUtilisateurResponse findById(
            Long idUtilisateur
    ) {
        return compteUtilisateurMapper.toResponse(
                findEntityById(idUtilisateur)
        );
    }

    @Transactional
    public CompteUtilisateurResponse create(
            CompteUtilisateurCreateRequest request
    ) {
        Personne personne = personneRepository
                .findById(request.idPersonne())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La personne ayant l'identifiant "
                                        + request.idPersonne()
                                        + " est introuvable."
                        )
                );

        validateRole(personne, request.role());

        if (compteUtilisateurRepository
                .existsByPersonneIdPersonne(
                        request.idPersonne()
                )) {

            throw new DuplicateResourceException(
                    "La personne ayant l'identifiant "
                            + request.idPersonne()
                            + " possède déjà un compte utilisateur."
            );
        }

        String emailConnexion =
                normalizeEmail(request.emailConnexion());

        if (compteUtilisateurRepository
                .existsByEmailConnexion(emailConnexion)) {

            throw new DuplicateResourceException(
                    "L'adresse électronique de connexion "
                            + emailConnexion
                            + " est déjà utilisée."
            );
        }

        String motDePasseHash = passwordEncoder.encode(
                request.motDePasse()
        );

        CompteUtilisateur compteUtilisateur =
                compteUtilisateurMapper.toEntity(
                        personne,
                        emailConnexion,
                        motDePasseHash,
                        request.role()
                );

        CompteUtilisateur savedCompteUtilisateur =
                compteUtilisateurRepository.save(
                        compteUtilisateur
                );

        return compteUtilisateurMapper.toResponse(
                savedCompteUtilisateur
        );
    }

    @Transactional
    public CompteUtilisateurResponse update(
            Long idUtilisateur,
            CompteUtilisateurUpdateRequest request
    ) {
        CompteUtilisateur compteUtilisateur =
                findEntityById(idUtilisateur);

        validateRole(compteUtilisateur.getPersonne(), request.role());

        String emailConnexion =
                normalizeEmail(request.emailConnexion());

        if (compteUtilisateurRepository
                .existsByEmailConnexionAndIdUtilisateurNot(
                        emailConnexion,
                        idUtilisateur
                )) {

            throw new DuplicateResourceException(
                    "L'adresse électronique de connexion "
                            + emailConnexion
                            + " est déjà utilisée."
            );
        }

        String nouveauMotDePasseHash = null;

        if (request.nouveauMotDePasse() != null) {
            nouveauMotDePasseHash = passwordEncoder.encode(
                    request.nouveauMotDePasse()
            );
        }

        compteUtilisateurMapper.updateEntity(
                compteUtilisateur,
                emailConnexion,
                request.role(),
                request.actif(),
                nouveauMotDePasseHash
        );

        CompteUtilisateur savedCompteUtilisateur =
                compteUtilisateurRepository.save(
                        compteUtilisateur
                );

        return compteUtilisateurMapper.toResponse(
                savedCompteUtilisateur
        );
    }

    @Transactional
    public void delete(Long idUtilisateur) {
        CompteUtilisateur compteUtilisateur =
                findEntityById(idUtilisateur);

        compteUtilisateurRepository.delete(
                compteUtilisateur
        );
    }

    private CompteUtilisateur findEntityById(
            Long idUtilisateur
    ) {
        return compteUtilisateurRepository
                .findById(idUtilisateur)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Le compte utilisateur ayant l'identifiant "
                                        + idUtilisateur
                                        + " est introuvable."
                        )
                );
    }

    private String normalizeEmail(String emailConnexion) {
        return emailConnexion
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void validateRole(Personne personne, RoleUtilisateur role) {
        if (role == RoleUtilisateur.ENSEIGNANT && !(personne instanceof Enseignant)) {
            throw new InvalidAccountRoleException("Le rôle ENSEIGNANT exige une personne enseignante.");
        }
        if (role == RoleUtilisateur.RESPONSABLE && !(personne instanceof Responsable)) {
            throw new InvalidAccountRoleException("Le rôle RESPONSABLE exige un responsable légal.");
        }
    }
}
