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

@Entity
@Table(
        name = "enseignement",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_enseignement_affectation",
                        columnNames = {
                                "id_enseignant",
                                "id_classe",
                                "id_matiere",
                                "date_debut"
                        }
                )
        }
)
public class Enseignement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_enseignement")
    private Long idEnseignement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_enseignant",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_enseignement_enseignant"
            )
    )
    private Enseignant enseignant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_classe",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_enseignement_classe"
            )
    )
    private Classe classe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_matiere",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_enseignement_matiere"
            )
    )
    private Matiere matiere;

    @Column(
            name = "coefficient_matiere",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal coefficientMatiere = BigDecimal.ONE;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    protected Enseignement() {
    }

    public Enseignement(
            Enseignant enseignant,
            Classe classe,
            Matiere matiere,
            BigDecimal coefficientMatiere,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        this.enseignant = enseignant;
        this.classe = classe;
        this.matiere = matiere;
        this.coefficientMatiere = coefficientMatiere;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.actif = true;
    }

    public Long getIdEnseignement() {
        return idEnseignement;
    }

    public Enseignant getEnseignant() {
        return enseignant;
    }

    public void setEnseignant(Enseignant enseignant) {
        this.enseignant = enseignant;
    }

    public Classe getClasse() {
        return classe;
    }

    public void setClasse(Classe classe) {
        this.classe = classe;
    }

    public Matiere getMatiere() {
        return matiere;
    }

    public void setMatiere(Matiere matiere) {
        this.matiere = matiere;
    }

    public BigDecimal getCoefficientMatiere() {
        return coefficientMatiere;
    }

    public void setCoefficientMatiere(BigDecimal coefficientMatiere) {
        this.coefficientMatiere = coefficientMatiere;
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

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}