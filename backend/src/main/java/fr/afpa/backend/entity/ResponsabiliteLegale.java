package fr.afpa.backend.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "responsabilite_legale")
public class ResponsabiliteLegale {

    @EmbeddedId
    private ResponsabiliteLegaleId id;

    @MapsId("idResponsable")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_responsable",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_responsabilite_legale_responsable"
            )
    )
    private Responsable responsable;

    @MapsId("idEleve")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_eleve",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_responsabilite_legale_eleve"
            )
    )
    private Eleve eleve;

    protected ResponsabiliteLegale() {
    }

    public ResponsabiliteLegale(
            Responsable responsable,
            Eleve eleve
    ) {
        this.responsable = Objects.requireNonNull(
                responsable,
                "Le responsable est obligatoire"
        );

        this.eleve = Objects.requireNonNull(
                eleve,
                "L'élève est obligatoire"
        );

        this.id = new ResponsabiliteLegaleId();
    }

    public ResponsabiliteLegaleId getId() {
        return id;
    }

    public Responsable getResponsable() {
        return responsable;
    }

    public Eleve getEleve() {
        return eleve;
    }
}