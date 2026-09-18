package fr.afpa.backend.service;

import fr.afpa.backend.dto.authentication.AuthenticationResponse;
import fr.afpa.backend.dto.authentication.LoginRequest;
import fr.afpa.backend.dto.authentication.RegisterRequest;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurCreateRequest;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurResponse;
import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.exception.ForbiddenOperationException;
import fr.afpa.backend.exception.UnauthorizedException;
import fr.afpa.backend.repository.CompteUtilisateurRepository;
import fr.afpa.backend.security.CompteUtilisateurDetailsService;
import fr.afpa.backend.security.CompteUtilisateurPrincipal;
import fr.afpa.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthenticationService {

    private static final String ADMIN_AUTHORITY =
            "ROLE_ADMIN";

    private final AuthenticationManager
            authenticationManager;

    private final CompteUtilisateurService
            compteUtilisateurService;

    private final CompteUtilisateurRepository
            compteUtilisateurRepository;

    private final CompteUtilisateurDetailsService
            compteUtilisateurDetailsService;

    private final JwtService jwtService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            CompteUtilisateurService compteUtilisateurService,
            CompteUtilisateurRepository compteUtilisateurRepository,
            CompteUtilisateurDetailsService compteUtilisateurDetailsService,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.compteUtilisateurService =
                compteUtilisateurService;

        this.compteUtilisateurRepository =
                compteUtilisateurRepository;

        this.compteUtilisateurDetailsService =
                compteUtilisateurDetailsService;

        this.jwtService = jwtService;
    }

    public AuthenticationResponse login(
            LoginRequest request
    ) {
        String normalizedEmail = normalizeEmail(
                request.emailConnexion()
        );

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            UsernamePasswordAuthenticationToken
                                    .unauthenticated(
                                            normalizedEmail,
                                            request.motDePasse()
                                    )
                    );

            return buildResponse(
                    extractPrincipal(authentication)
            );
        }
        catch (AuthenticationException exception) {
            throw new UnauthorizedException(
                    "Les identifiants de connexion sont invalides."
            );
        }
    }

    public AuthenticationResponse register(
            RegisterRequest request,
            Authentication currentAuthentication
    ) {
        boolean firstAccount =
                compteUtilisateurRepository.count() == 0;

        if (firstAccount
                && request.role() != RoleUtilisateur.ADMIN) {

            throw new ForbiddenOperationException(
                    "Le premier compte utilisateur doit avoir le rôle ADMIN."
            );
        }

        if (!firstAccount
                && (currentAuthentication == null
                || !currentAuthentication.isAuthenticated()
                || currentAuthentication instanceof AnonymousAuthenticationToken)) {
            throw new UnauthorizedException(
                    "Une authentification est nécessaire pour créer un compte utilisateur."
            );
        }

        if (!firstAccount
                && !isAdministrator(
                        currentAuthentication
                )) {

            throw new ForbiddenOperationException(
                    "Seul un administrateur peut créer un nouveau compte utilisateur."
            );
        }

        CompteUtilisateurResponse createdAccount =
                compteUtilisateurService.create(
                        new CompteUtilisateurCreateRequest(
                                request.idPersonne(),
                                request.emailConnexion(),
                                request.motDePasse(),
                                request.role()
                        )
                );

        UserDetails userDetails =
                compteUtilisateurDetailsService
                        .loadUserByUsername(
                                createdAccount.emailConnexion()
                        );

        if (!(userDetails
                instanceof CompteUtilisateurPrincipal principal)) {

            throw new IllegalStateException(
                    "Le principal de sécurité du compte utilisateur est invalide."
            );
        }

        return buildResponse(principal);
    }

    private AuthenticationResponse buildResponse(
            CompteUtilisateurPrincipal principal
    ) {
        return new AuthenticationResponse(
                jwtService.generateToken(principal),
                "Bearer",
                jwtService.getExpirationSeconds(),
                principal.getIdUtilisateur(),
                principal.getIdPersonne(),
                principal.getUsername(),
                principal.getRole()
        );
    }

    private CompteUtilisateurPrincipal extractPrincipal(
            Authentication authentication
    ) {
        Object principal = authentication.getPrincipal();

        if (!(principal
                instanceof CompteUtilisateurPrincipal
                compteUtilisateurPrincipal)) {

            throw new IllegalStateException(
                    "Le principal de sécurité du compte utilisateur est invalide."
            );
        }

        return compteUtilisateurPrincipal;
    }

    private boolean isAdministrator(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {

            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        ADMIN_AUTHORITY.equals(
                                authority.getAuthority()
                        )
                );
    }

    private String normalizeEmail(
            String emailConnexion
    ) {
        return emailConnexion
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
