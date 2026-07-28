package fr.afpa.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "matiere",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_matiere_code",
                        columnNames = "code"
                )
        }
)
public class Matiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_matiere")
    private Long idMatiere;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    protected Matiere() {
    }

    public Matiere(String code, String nom) {
        this.code = code;
        this.nom = nom;
    }

    public Long getIdMatiere() {
        return idMatiere;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }
}