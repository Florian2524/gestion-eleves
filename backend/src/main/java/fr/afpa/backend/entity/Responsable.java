package fr.afpa.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "responsable")
@PrimaryKeyJoinColumn(
        name = "id_personne",
        referencedColumnName = "id_personne"
)
public class Responsable extends Personne {

    @Column(name = "profession", length = 150)
    private String profession;

    protected Responsable() {
    }

    public Responsable(
            String nom,
            String prenom,
            String emailContact,
            String telephone,
            String adresse,
            String profession
    ) {
        super(nom, prenom, emailContact, telephone, adresse);
        this.profession = profession;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }
}