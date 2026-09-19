package fr.afpa.backend.security;

import fr.afpa.backend.entity.CompteUtilisateur;
import fr.afpa.backend.repository.CompteUtilisateurRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {
    private final CompteUtilisateurRepository accounts;

    public ActiveAccountJwtValidator(CompteUtilisateurRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object claim = jwt.getClaim("idUtilisateur");
        if (claim instanceof Number number) {
            var account = accounts.findWithPersonneByIdUtilisateur(number.longValue());
            if (account.isPresent() && matches(account.get(), jwt)) {
                return OAuth2TokenValidatorResult.success();
            }
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Compte inactif ou jeton obsolète.", null));
    }

    private boolean matches(CompteUtilisateur account, Jwt jwt) {
        Object personId = jwt.getClaim("idPersonne");
        List<String> roles = jwt.getClaimAsStringList("roles");
        return account.isActif()
                && account.getEmailConnexion().equals(jwt.getSubject())
                && personId instanceof Number number
                && account.getPersonne().getIdPersonne().equals(number.longValue())
                && roles != null && roles.size() == 1
                && roles.getFirst().equals(account.getRole().name());
    }
}
