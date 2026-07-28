package fr.afpa.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "classe",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_classe_nom_annee",
                        columnNames = {"nom", "annee_scolaire"}
                )
        }
)
public class Classe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_classe")
    private Long idClasse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_professeur_principal",
            nullable = true,
            foreignKey = @ForeignKey(
                    name = "fk_classe_professeur_principal"
            )
    )
    private Enseignant professeurPrincipal;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "niveau", nullable = false, length = 50)
    private String niveau;

    @Column(name = "annee_scolaire", nullable = false, length = 20)
    private String anneeScolaire;

    protected Classe() {
    }

    public Classe(
            String nom,
            String niveau,
            String anneeScolaire,
            Enseignant professeurPrincipal
    ) {
        this.nom = nom;
        this.niveau = niveau;
        this.anneeScolaire = anneeScolaire;
        this.professeurPrincipal = professeurPrincipal;
    }

    public Long getIdClasse() {
        return idClasse;
    }

    public Enseignant getProfesseurPrincipal() {
        return professeurPrincipal;
    }

    public void setProfesseurPrincipal(Enseignant professeurPrincipal) {
        this.professeurPrincipal = professeurPrincipal;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getAnneeScolaire() {
        return anneeScolaire;
    }

    public void setAnneeScolaire(String anneeScolaire) {
        this.anneeScolaire = anneeScolaire;
    }
}