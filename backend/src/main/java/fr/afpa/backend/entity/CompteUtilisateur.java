package fr.afpa.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "compte_utilisateur",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_compte_utilisateur_personne",
                        columnNames = "id_personne"
                ),
                @UniqueConstraint(
                        name = "uq_compte_utilisateur_email",
                        columnNames = "email_connexion"
                )
        }
)
public class CompteUtilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private Long idUtilisateur;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_personne",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_compte_utilisateur_personne"
            )
    )
    private Personne personne;

    @Column(
            name = "email_connexion",
            nullable = false,
            unique = true,
            length = 255
    )
    private String emailConnexion;

    @Column(name = "mot_de_passe_hash", nullable = false, length = 255)
    private String motDePasseHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private RoleUtilisateur role;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    protected CompteUtilisateur() {
    }

    public CompteUtilisateur(
            Personne personne,
            String emailConnexion,
            String motDePasseHash,
            RoleUtilisateur role
    ) {
        this.personne = personne;
        this.emailConnexion = emailConnexion;
        this.motDePasseHash = motDePasseHash;
        this.role = role;
        this.actif = true;
    }

    public Long getIdUtilisateur() {
        return idUtilisateur;
    }

    public Personne getPersonne() {
        return personne;
    }

    public void setPersonne(Personne personne) {
        this.personne = personne;
    }

    public String getEmailConnexion() {
        return emailConnexion;
    }

    public void setEmailConnexion(String emailConnexion) {
        this.emailConnexion = emailConnexion;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public RoleUtilisateur getRole() {
        return role;
    }

    public void setRole(RoleUtilisateur role) {
        this.role = role;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }
}