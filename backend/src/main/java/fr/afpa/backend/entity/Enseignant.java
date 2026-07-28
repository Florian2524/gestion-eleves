package fr.afpa.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "enseignant")
@PrimaryKeyJoinColumn(
        name = "id_personne",
        referencedColumnName = "id_personne"
)
public class Enseignant extends Personne {

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
            String nom,
            String prenom,
            String emailContact,
            String telephone,
            String adresse,
            String numeroEmploye
    ) {
        super(nom, prenom, emailContact, telephone, adresse);
        this.numeroEmploye = numeroEmploye;
    }

    public String getNumeroEmploye() {
        return numeroEmploye;
    }

    public void setNumeroEmploye(String numeroEmploye) {
        this.numeroEmploye = numeroEmploye;
    }
}