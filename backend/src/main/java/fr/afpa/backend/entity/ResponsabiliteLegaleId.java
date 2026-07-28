package fr.afpa.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ResponsabiliteLegaleId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_responsable")
    private Long idResponsable;

    @Column(name = "id_eleve")
    private Long idEleve;

    protected ResponsabiliteLegaleId() {
    }

    public ResponsabiliteLegaleId(
            Long idResponsable,
            Long idEleve
    ) {
        this.idResponsable = idResponsable;
        this.idEleve = idEleve;
    }

    public Long getIdResponsable() {
        return idResponsable;
    }

    public Long getIdEleve() {
        return idEleve;
    }

    @Override
    public boolean equals(Object objet) {
        if (this == objet) {
            return true;
        }

        if (!(objet instanceof ResponsabiliteLegaleId autre)) {
            return false;
        }

        return Objects.equals(idResponsable, autre.idResponsable)
                && Objects.equals(idEleve, autre.idEleve);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idResponsable, idEleve);
    }
}