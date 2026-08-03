package fr.afpa.backend.security;

import fr.afpa.backend.entity.CompteUtilisateur;
import fr.afpa.backend.repository.CompteUtilisateurRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CompteUtilisateurDetailsService
        implements UserDetailsService {

    private final CompteUtilisateurRepository
            compteUtilisateurRepository;

    public CompteUtilisateurDetailsService(
            CompteUtilisateurRepository compteUtilisateurRepository
    ) {
        this.compteUtilisateurRepository =
                compteUtilisateurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String emailConnexion
    ) throws UsernameNotFoundException {

        String normalizedEmail = emailConnexion
                .trim()
                .toLowerCase(Locale.ROOT);

        CompteUtilisateur compteUtilisateur =
                compteUtilisateurRepository
                        .findByEmailConnexion(normalizedEmail)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "Le compte utilisateur est introuvable."
                                )
                        );

        return CompteUtilisateurPrincipal.fromEntity(
                compteUtilisateur
        );
    }
}
