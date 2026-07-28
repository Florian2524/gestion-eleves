package fr.afpa.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "periode")
public class Periode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_periode")
    private Long idPeriode;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "date_debut_saisie")
    private LocalDate dateDebutSaisie;

    @Column(name = "date_fin_saisie")
    private LocalDate dateFinSaisie;

    @Column(name = "statut", nullable = false, length = 30)
    private String statut;

    protected Periode() {
    }

    public Periode(
            String libelle,
            LocalDate dateDebut,
            LocalDate dateFin,
            LocalDate dateDebutSaisie,
            LocalDate dateFinSaisie,
            String statut
    ) {
        this.libelle = libelle;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.dateDebutSaisie = dateDebutSaisie;
        this.dateFinSaisie = dateFinSaisie;
        this.statut = statut;
    }

    public Long getIdPeriode() {
        return idPeriode;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public LocalDate getDateDebutSaisie() {
        return dateDebutSaisie;
    }

    public void setDateDebutSaisie(LocalDate dateDebutSaisie) {
        this.dateDebutSaisie = dateDebutSaisie;
    }

    public LocalDate getDateFinSaisie() {
        return dateFinSaisie;
    }

    public void setDateFinSaisie(LocalDate dateFinSaisie) {
        this.dateFinSaisie = dateFinSaisie;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}