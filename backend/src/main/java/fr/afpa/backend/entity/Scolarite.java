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

import java.time.LocalDate;

@Entity
@Table(
        name = "scolarite",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_scolarite_eleve_classe_debut",
                        columnNames = {
                                "id_eleve",
                                "id_classe",
                                "date_debut"
                        }
                )
        }
)
public class Scolarite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_scolarite")
    private Long idScolarite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_eleve",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_scolarite_eleve")
    )
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_classe",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_scolarite_classe")
    )
    private Classe classe;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "statut", nullable = false, length = 30)
    private String statut;

    protected Scolarite() {
    }

    public Scolarite(
            Eleve eleve,
            Classe classe,
            LocalDate dateDebut,
            LocalDate dateFin,
            String statut
    ) {
        this.eleve = eleve;
        this.classe = classe;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
    }

    public Long getIdScolarite() {
        return idScolarite;
    }

    public Eleve getEleve() {
        return eleve;
    }

    public void setEleve(Eleve eleve) {
        this.eleve = eleve;
    }

    public Classe getClasse() {
        return classe;
    }

    public void setClasse(Classe classe) {
        this.classe = classe;
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

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}