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

@Entity
@Table(name = "responsable")
public class Responsable {

    @Id
    @Column(name = "id_personne")
    private Long idPersonne;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id_personne", nullable = false,
            foreignKey = @ForeignKey(name = "fk_responsable_personne"))
    private Personne personne;

    public Long getIdPersonne() {
        return idPersonne;
    }

    public Personne getPersonne() {
        return personne;
    }

    @Column(name = "profession", length = 150)
    private String profession;

    protected Responsable() {
    }

    public Responsable(
            Personne personne,
            String profession
    ) {
        this.personne = Objects.requireNonNull(personne, "La personne est obligatoire");
        this.profession = profession;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }
}
