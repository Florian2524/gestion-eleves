package fr.afpa.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PondererId implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column(name = "id_classe")
    private Long idClasse;
    @Column(name = "id_matiere")
    private Long idMatiere;

    protected PondererId() { }
    public PondererId(Long idClasse, Long idMatiere) {
        this.idClasse = idClasse;
        this.idMatiere = idMatiere;
    }
    public Long getIdClasse() { return idClasse; }
    public Long getIdMatiere() { return idMatiere; }
    @Override public boolean equals(Object obj) {
        return this == obj || obj instanceof PondererId other
                && Objects.equals(idClasse, other.idClasse)
                && Objects.equals(idMatiere, other.idMatiere);
    }
    @Override public int hashCode() { return Objects.hash(idClasse, idMatiere); }
}
