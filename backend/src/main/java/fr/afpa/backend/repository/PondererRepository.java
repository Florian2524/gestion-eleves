package fr.afpa.backend.repository;

import fr.afpa.backend.entity.Ponderer;
import fr.afpa.backend.entity.PondererId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PondererRepository extends JpaRepository<Ponderer, PondererId> {
    List<Ponderer> findAllById_IdClasse(Long idClasse);
    boolean existsById_IdClasse(Long idClasse);
    boolean existsById_IdMatiere(Long idMatiere);

    // Même ordre que la cascade : verrou de référence avant toute écriture d'enseignement.
    @Query(value = "SELECT * FROM ponderer WHERE id_classe = :#{#id.idClasse} AND id_matiere = :#{#id.idMatiere} FOR KEY SHARE", nativeQuery = true)
    Optional<Ponderer> findForShare(@Param("id") PondererId id);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO ponderer(id_classe, id_matiere, coefficient_matiere)
            VALUES (:idClasse, :idMatiere, :coefficient)
            ON CONFLICT (id_classe, id_matiere) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("idClasse") Long idClasse, @Param("idMatiere") Long idMatiere,
                       @Param("coefficient") BigDecimal coefficient);

    // La FK cascade vers le miroir V1 ; vider le contexte évite ses valeurs périmées.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Ponderer p set p.coefficientMatiere = :coefficient where p.id = :id")
    int updateCoefficient(@Param("id") PondererId id, @Param("coefficient") BigDecimal coefficient);
}
