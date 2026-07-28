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

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "ligne_bulletin",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_ligne_bulletin_enseignement",
                        columnNames = {
                                "id_bulletin",
                                "id_enseignement"
                        }
                )
        }
)
public class LigneBulletin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ligne_bulletin")
    private Long idLigneBulletin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_bulletin",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_ligne_bulletin_bulletin"
            )
    )
    private Bulletin bulletin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_enseignement",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_ligne_bulletin_enseignement"
            )
    )
    private Enseignement enseignement;

    @Column(name = "texte", columnDefinition = "TEXT")
    private String texte;

    @Column(name = "date_saisie")
    private OffsetDateTime dateSaisie;

    @Column(name = "date_modification")
    private OffsetDateTime dateModification;

    @Column(name = "statut", nullable = false, length = 30)
    private String statut;

    protected LigneBulletin() {
    }

    public LigneBulletin(
            Bulletin bulletin,
            Enseignement enseignement,
            String texte,
            String statut
    ) {
        this.bulletin = bulletin;
        this.enseignement = enseignement;
        this.texte = texte;
        this.statut = statut;
    }

    public Long getIdLigneBulletin() {
        return idLigneBulletin;
    }

    public Bulletin getBulletin() {
        return bulletin;
    }

    public void setBulletin(Bulletin bulletin) {
        this.bulletin = bulletin;
    }

    public Enseignement getEnseignement() {
        return enseignement;
    }

    public void setEnseignement(Enseignement enseignement) {
        this.enseignement = enseignement;
    }

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public OffsetDateTime getDateSaisie() {
        return dateSaisie;
    }

    public void setDateSaisie(OffsetDateTime dateSaisie) {
        this.dateSaisie = dateSaisie;
    }

    public OffsetDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(OffsetDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}