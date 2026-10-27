package fr.afpa.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import java.util.Objects;
import jakarta.persistence.Table;

@Entity
@Table(name = "enseignant")
public class Enseignant {

    @Id
    @Column(name = "id_personne")
    private Long idPersonne;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id_personne", nullable = false,
            foreignKey = @ForeignKey(name = "fk_enseignant_personne"))
    private Personne personne;

    public Long getIdPersonne() {
        return idPersonne;
    }

    public Personne getPersonne() {
        return personne;
    }

    @Column(
            name = "numero_employe",
            nullable = false,
            unique = true,
            length = 50
    )
    private String numeroEmploye;

    protected Enseignant() {
    }

    public Enseignant(
            Personne personne,
            String numeroEmploye
    ) {
        this.personne = Objects.requireNonNull(personne, "La personne est obligatoire");
        this.numeroEmploye = numeroEmploye;
    }

    public String getNumeroEmploye() {
        return numeroEmploye;
    }

    public void setNumeroEmploye(String numeroEmploye) {
        this.numeroEmploye = numeroEmploye;
    }
}
