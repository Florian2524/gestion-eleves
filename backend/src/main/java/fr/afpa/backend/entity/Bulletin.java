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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "bulletin",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_bulletin_scolarite_periode",
                        columnNames = {
                                "id_scolarite",
                                "id_periode"
                        }
                )
        }
)
public class Bulletin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bulletin")
    private Long idBulletin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_scolarite",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_bulletin_scolarite"
            )
    )
    private Scolarite scolarite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_periode",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_bulletin_periode"
            )
    )
    private Periode periode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_validateur",
            foreignKey = @ForeignKey(
                    name = "fk_bulletin_validateur"
            )
    )
    private Enseignant validateur;

    @Column(name = "date_generation")
    private LocalDate dateGeneration;

    @Column(
            name = "moyenne_generale",
            precision = 5,
            scale = 2
    )
    private BigDecimal moyenneGenerale;

    @Column(name = "appreciation_generale", columnDefinition = "TEXT")
    private String appreciationGenerale;

    @Column(name = "statut", nullable = false, length = 30)
    private String statut;

    @Column(name = "date_validation")
    private OffsetDateTime dateValidation;

    protected Bulletin() {
    }

    public Bulletin(
            Scolarite scolarite,
            Periode periode,
            String statut
    ) {
        this.scolarite = scolarite;
        this.periode = periode;
        this.statut = statut;
    }

    public Long getIdBulletin() {
        return idBulletin;
    }

    public Scolarite getScolarite() {
        return scolarite;
    }

    public void setScolarite(Scolarite scolarite) {
        this.scolarite = scolarite;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public Enseignant getValidateur() {
        return validateur;
    }

    public void setValidateur(Enseignant validateur) {
        this.validateur = validateur;
    }

    public LocalDate getDateGeneration() {
        return dateGeneration;
    }

    public void setDateGeneration(LocalDate dateGeneration) {
        this.dateGeneration = dateGeneration;
    }

    public BigDecimal getMoyenneGenerale() {
        return moyenneGenerale;
    }

    public void setMoyenneGenerale(BigDecimal moyenneGenerale) {
        this.moyenneGenerale = moyenneGenerale;
    }

    public String getAppreciationGenerale() {
        return appreciationGenerale;
    }

    public void setAppreciationGenerale(String appreciationGenerale) {
        this.appreciationGenerale = appreciationGenerale;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public OffsetDateTime getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(OffsetDateTime dateValidation) {
        this.dateValidation = dateValidation;
    }
}