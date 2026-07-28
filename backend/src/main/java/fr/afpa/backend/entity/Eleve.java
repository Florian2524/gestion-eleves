package fr.afpa.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "eleve")
@PrimaryKeyJoinColumn(
        name = "id_personne",
        referencedColumnName = "id_personne"
)
public class Eleve extends Personne {

    @Column(name = "matricule", nullable = false, unique = true, length = 50)
    private String matricule;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    protected Eleve() {
    }

    public Eleve(
            String nom,
            String prenom,
            String emailContact,
            String telephone,
            String adresse,
            String matricule,
            LocalDate dateNaissance,
            String photoUrl
    ) {
        super(nom, prenom, emailContact, telephone, adresse);
        this.matricule = matricule;
        this.dateNaissance = dateNaissance;
        this.photoUrl = photoUrl;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}