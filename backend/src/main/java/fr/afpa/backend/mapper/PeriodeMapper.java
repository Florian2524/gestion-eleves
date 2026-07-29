package fr.afpa.backend.mapper;

import fr.afpa.backend.dto.periode.PeriodeRequest;
import fr.afpa.backend.dto.periode.PeriodeResponse;
import fr.afpa.backend.entity.Periode;
import org.springframework.stereotype.Component;

@Component
public class PeriodeMapper {

    public Periode toEntity(PeriodeRequest request) {
        return new Periode(
                request.libelle(),
                request.dateDebut(),
                request.dateFin(),
                request.dateDebutSaisie(),
                request.dateFinSaisie(),
                request.statut()
        );
    }

    public PeriodeResponse toResponse(Periode periode) {
        return new PeriodeResponse(
                periode.getIdPeriode(),
                periode.getLibelle(),
                periode.getDateDebut(),
                periode.getDateFin(),
                periode.getDateDebutSaisie(),
                periode.getDateFinSaisie(),
                periode.getStatut()
        );
    }

    public void updateEntity(
            PeriodeRequest request,
            Periode periode
    ) {
        periode.setLibelle(request.libelle());
        periode.setDateDebut(request.dateDebut());
        periode.setDateFin(request.dateFin());
        periode.setDateDebutSaisie(request.dateDebutSaisie());
        periode.setDateFinSaisie(request.dateFinSaisie());
        periode.setStatut(request.statut());
    }
}