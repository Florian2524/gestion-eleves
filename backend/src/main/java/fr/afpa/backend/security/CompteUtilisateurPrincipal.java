package fr.afpa.backend.security;

import fr.afpa.backend.entity.CompteUtilisateur;
import fr.afpa.backend.entity.RoleUtilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class CompteUtilisateurPrincipal
        implements UserDetails {

    private final Long idUtilisateur;
    private final Long idPersonne;
    private final String emailConnexion;
    private final String motDePasseHash;
    private final RoleUtilisateur role;
    private final boolean actif;

    public CompteUtilisateurPrincipal(
            Long idUtilisateur,
            Long idPersonne,
            String emailConnexion,
            String motDePasseHash,
            RoleUtilisateur role,
            boolean actif
    ) {
        this.idUtilisateur = idUtilisateur;
        this.idPersonne = idPersonne;
        this.emailConnexion = emailConnexion;
        this.motDePasseHash = motDePasseHash;
        this.role = role;
        this.actif = actif;
    }

    public static CompteUtilisateurPrincipal fromEntity(
            CompteUtilisateur compteUtilisateur
    ) {
        return new CompteUtilisateurPrincipal(
                compteUtilisateur.getIdUtilisateur(),
                compteUtilisateur.getPersonne().getIdPersonne(),
                compteUtilisateur.getEmailConnexion(),
                compteUtilisateur.getMotDePasseHash(),
                compteUtilisateur.getRole(),
                compteUtilisateur.isActif()
        );
    }

    public Long getIdUtilisateur() {
        return idUtilisateur;
    }

    public Long getIdPersonne() {
        return idPersonne;
    }

    public RoleUtilisateur getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority>
    getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + role.name()
                )
        );
    }

    @Override
    public String getPassword() {
        return motDePasseHash;
    }

    @Override
    public String getUsername() {
        return emailConnexion;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}
