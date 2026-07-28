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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "note",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_note_scolarite_evaluation",
                        columnNames = {
                                "id_scolarite",
                                "id_evaluation"
                        }
                )
        }
)
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_note")
    private Long idNote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_scolarite",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_note_scolarite"
            )
    )
    private Scolarite scolarite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_evaluation",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_note_evaluation"
            )
    )
    private Evaluation evaluation;

    @Column(
            name = "valeur",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal valeur;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "statut_note", nullable = false, length = 30)
    private String statutNote;

    @CreationTimestamp
    @Column(
            name = "date_saisie",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime dateSaisie;

    protected Note() {
    }

    public Note(
            Scolarite scolarite,
            Evaluation evaluation,
            BigDecimal valeur,
            String commentaire,
            String statutNote
    ) {
        this.scolarite = scolarite;
        this.evaluation = evaluation;
        this.valeur = valeur;
        this.commentaire = commentaire;
        this.statutNote = statutNote;
    }

    public Long getIdNote() {
        return idNote;
    }

    public Scolarite getScolarite() {
        return scolarite;
    }

    public void setScolarite(Scolarite scolarite) {
        this.scolarite = scolarite;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public BigDecimal getValeur() {
        return valeur;
    }

    public void setValeur(BigDecimal valeur) {
        this.valeur = valeur;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public String getStatutNote() {
        return statutNote;
    }

    public void setStatutNote(String statutNote) {
        this.statutNote = statutNote;
    }

    public OffsetDateTime getDateSaisie() {
        return dateSaisie;
    }
}