package fr.afpa.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "ponderer")
public class Ponderer {
    @EmbeddedId
    private PondererId id;
    @MapsId("idClasse")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_classe", foreignKey = @ForeignKey(name = "fk_ponderer_classe"))
    private Classe classe;
    @MapsId("idMatiere")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_matiere", foreignKey = @ForeignKey(name = "fk_ponderer_matiere"))
    private Matiere matiere;
    @Column(name = "coefficient_matiere", nullable = false, precision = 5, scale = 2)
    private BigDecimal coefficientMatiere;

    protected Ponderer() { }
    public Ponderer(Classe classe, Matiere matiere, BigDecimal coefficientMatiere) {
        this.classe = Objects.requireNonNull(classe);
        this.matiere = Objects.requireNonNull(matiere);
        this.id = new PondererId(classe.getIdClasse(), matiere.getIdMatiere());
        this.coefficientMatiere = coefficientMatiere;
    }
    public PondererId getId() { return id; }
    public Classe getClasse() { return classe; }
    public Matiere getMatiere() { return matiere; }
    public BigDecimal getCoefficientMatiere() { return coefficientMatiere; }
}
