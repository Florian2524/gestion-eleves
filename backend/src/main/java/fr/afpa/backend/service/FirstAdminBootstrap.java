package fr.afpa.backend.service;

import fr.afpa.backend.entity.CompteUtilisateur;
import fr.afpa.backend.entity.Personne;
import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.repository.CompteUtilisateurRepository;
import fr.afpa.backend.repository.PersonneRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class FirstAdminBootstrap implements ApplicationRunner {
    private final CompteUtilisateurRepository comptes;
    private final PersonneRepository personnes;
    private final PasswordEncoder encoder;

    @Value("${application.bootstrap.admin.email:}") private String email;
    @Value("${application.bootstrap.admin.password:}") private String password;
    @Value("${application.bootstrap.admin.nom:}") private String nom;
    @Value("${application.bootstrap.admin.prenom:}") private String prenom;

    public FirstAdminBootstrap(CompteUtilisateurRepository comptes,
                               PersonneRepository personnes, PasswordEncoder encoder) {
        this.comptes = comptes;
        this.personnes = personnes;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (comptes.count() != 0) return;
        if (email.isBlank() && password.isBlank() && nom.isBlank() && prenom.isBlank()) return;
        if (email.isBlank() || password.length() < 8 || nom.isBlank() || prenom.isBlank()) {
            throw new IllegalStateException("Configuration du premier administrateur incomplète.");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (comptes.existsByEmailConnexion(normalizedEmail)) {
            throw new IllegalStateException("L'adresse du premier administrateur est déjà utilisée.");
        }
        Personne personne = personnes.save(new Personne(nom, prenom, normalizedEmail, null, null));
        comptes.save(new CompteUtilisateur(personne, normalizedEmail,
                encoder.encode(password), RoleUtilisateur.ADMIN));
    }
}
