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

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "evaluation")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluation")
    private Long idEvaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_enseignement",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_evaluation_enseignement"
            )
    )
    private Enseignement enseignement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_periode",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_evaluation_periode"
            )
    )
    private Periode periode;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;

    @Column(name = "type_evaluation", nullable = false, length = 50)
    private String typeEvaluation;

    @Column(
            name = "coefficient_evaluation",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal coefficientEvaluation = BigDecimal.ONE;

    @Column(
            name = "bareme",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal bareme = BigDecimal.valueOf(20);

    protected Evaluation() {
    }

    public Evaluation(
            Enseignement enseignement,
            Periode periode,
            String libelle,
            LocalDate dateEvaluation,
            String typeEvaluation,
            BigDecimal coefficientEvaluation,
            BigDecimal bareme
    ) {
        this.enseignement = enseignement;
        this.periode = periode;
        this.libelle = libelle;
        this.dateEvaluation = dateEvaluation;
        this.typeEvaluation = typeEvaluation;
        this.coefficientEvaluation = coefficientEvaluation;
        this.bareme = bareme;
    }

    public Long getIdEvaluation() {
        return idEvaluation;
    }

    public Enseignement getEnseignement() {
        return enseignement;
    }

    public void setEnseignement(Enseignement enseignement) {
        this.enseignement = enseignement;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public LocalDate getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(LocalDate dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }

    public String getTypeEvaluation() {
        return typeEvaluation;
    }

    public void setTypeEvaluation(String typeEvaluation) {
        this.typeEvaluation = typeEvaluation;
    }

    public BigDecimal getCoefficientEvaluation() {
        return coefficientEvaluation;
    }

    public void setCoefficientEvaluation(BigDecimal coefficientEvaluation) {
        this.coefficientEvaluation = coefficientEvaluation;
    }

    public BigDecimal getBareme() {
        return bareme;
    }

    public void setBareme(BigDecimal bareme) {
        this.bareme = bareme;
    }
}