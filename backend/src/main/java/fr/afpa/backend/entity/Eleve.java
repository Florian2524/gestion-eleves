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

import java.time.LocalDate;

@Entity
@Table(name = "eleve")
public class Eleve {

    @Id
    @Column(name = "id_personne")
    private Long idPersonne;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id_personne", nullable = false,
            foreignKey = @ForeignKey(name = "fk_eleve_personne"))
    private Personne personne;

    public Long getIdPersonne() {
        return idPersonne;
    }

    public Personne getPersonne() {
        return personne;
    }

    @Column(name = "matricule", nullable = false, unique = true, length = 50)
    private String matricule;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    protected Eleve() {
    }

    public Eleve(
            Personne personne,
            String matricule,
            LocalDate dateNaissance,
            String photoUrl
    ) {
        this.personne = Objects.requireNonNull(personne, "La personne est obligatoire");
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
