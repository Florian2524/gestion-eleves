package fr.afpa.backend.service;

import fr.afpa.backend.dto.bulletin.BulletinDto;
import fr.afpa.backend.dto.bulletin.LigneBulletinDto;
import fr.afpa.backend.entity.Classe;
import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.Enseignement;
import fr.afpa.backend.entity.Evaluation;
import fr.afpa.backend.entity.Matiere;
import fr.afpa.backend.entity.Note;
import fr.afpa.backend.entity.Periode;
import fr.afpa.backend.entity.Scolarite;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.repository.EnseignementRepository;
import fr.afpa.backend.repository.NoteRepository;
import fr.afpa.backend.repository.PeriodeRepository;
import fr.afpa.backend.repository.ScolariteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class BulletinCalculService {

    private static final BigDecimal NOTE_SUR_VINGT =
            BigDecimal.valueOf(20);

    private static final int ECHELLE_CALCUL = 10;
    private static final int ECHELLE_AFFICHAGE = 2;

    private final ScolariteRepository scolariteRepository;
    private final PeriodeRepository periodeRepository;
    private final EnseignementRepository enseignementRepository;
    private final NoteRepository noteRepository;

    public BulletinCalculService(
            ScolariteRepository scolariteRepository,
            PeriodeRepository periodeRepository,
            EnseignementRepository enseignementRepository,
            NoteRepository noteRepository
    ) {
        this.scolariteRepository = scolariteRepository;
        this.periodeRepository = periodeRepository;
        this.enseignementRepository = enseignementRepository;
        this.noteRepository = noteRepository;
    }

    public BulletinDto calculer(
            Long idScolarite,
            Long idPeriode
    ) {
        Scolarite scolarite =
                findScolariteById(idScolarite);

        Periode periode =
                findPeriodeById(idPeriode);

        Classe classe = scolarite.getClasse();
        Eleve eleve = scolarite.getEleve();

        List<Enseignement> enseignements =
                enseignementRepository
                        .findAllByClasse_IdClasse(
                                classe.getIdClasse()
                        )
                        .stream()
                        .filter(enseignement ->
                                concernePeriode(
                                        enseignement,
                                        periode
                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        this::nomMatiere,
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();

        List<Note> notes =
                noteRepository
                        .findAllByScolarite_IdScolariteAndEvaluation_Periode_IdPeriode(
                                idScolarite,
                                idPeriode
                        );

        Map<Long, List<Note>> notesParEnseignement =
                regrouperNotesParEnseignement(notes);

        List<LigneBulletinDto> lignes =
                new ArrayList<>();

        BigDecimal sommeMoyennesPonderees =
                BigDecimal.ZERO;

        BigDecimal sommeCoefficientsMatieres =
                BigDecimal.ZERO;

        for (Enseignement enseignement : enseignements) {
            Long idEnseignement =
                    enseignement.getIdEnseignement();

            List<Note> notesEnseignement =
                    notesParEnseignement.getOrDefault(
                            idEnseignement,
                            List.of()
                    );

            ResultatMoyenne resultatMoyenne =
                    calculerMoyenneMatiere(
                            notesEnseignement
                    );

            Matiere matiere =
                    enseignement.getMatiere();

            BigDecimal coefficientMatiere =
                    enseignement.getCoefficientMatiere();

            lignes.add(
                    new LigneBulletinDto(
                            idEnseignement,
                            matiere.getIdMatiere(),
                            matiere.getCode(),
                            matiere.getNom(),
                            coefficientMatiere,
                            resultatMoyenne.nombreNotes(),
                            resultatMoyenne.moyenneArrondie()
                    )
            );

            if (resultatMoyenne.moyenneBrute() != null
                    && estStrictementPositif(
                    coefficientMatiere
            )) {
                sommeMoyennesPonderees =
                        sommeMoyennesPonderees.add(
                                resultatMoyenne
                                        .moyenneBrute()
                                        .multiply(
                                                coefficientMatiere
                                        )
                        );

                sommeCoefficientsMatieres =
                        sommeCoefficientsMatieres.add(
                                coefficientMatiere
                        );
            }
        }

        BigDecimal moyenneGenerale =
                calculerMoyenneGenerale(
                        sommeMoyennesPonderees,
                        sommeCoefficientsMatieres
                );

        return new BulletinDto(
                idScolarite,
                idPeriode,
                eleve.getIdPersonne(),
                eleve.getMatricule(),
                eleve.getNom(),
                eleve.getPrenom(),
                classe.getIdClasse(),
                classe.getNom(),
                classe.getNiveau(),
                classe.getAnneeScolaire(),
                periode.getLibelle(),
                periode.getDateDebut(),
                periode.getDateFin(),
                lignes,
                moyenneGenerale
        );
    }

    private Map<Long, List<Note>>
    regrouperNotesParEnseignement(
            List<Note> notes
    ) {
        Map<Long, List<Note>> resultat =
                new HashMap<>();

        for (Note note : notes) {
            if (note == null
                    || note.getEvaluation() == null
                    || note.getEvaluation()
                    .getEnseignement() == null) {
                continue;
            }

            Long idEnseignement =
                    note.getEvaluation()
                            .getEnseignement()
                            .getIdEnseignement();

            if (idEnseignement == null) {
                continue;
            }

            resultat.computeIfAbsent(
                    idEnseignement,
                    ignored -> new ArrayList<>()
            ).add(note);
        }

        return resultat;
    }

    private ResultatMoyenne calculerMoyenneMatiere(
            List<Note> notes
    ) {
        BigDecimal sommeNotesPonderees =
                BigDecimal.ZERO;

        BigDecimal sommeCoefficientsEvaluations =
                BigDecimal.ZERO;

        int nombreNotes = 0;

        for (Note note : notes) {
            Evaluation evaluation =
                    note.getEvaluation();

            BigDecimal valeur =
                    note.getValeur();

            BigDecimal bareme =
                    evaluation.getBareme();

            BigDecimal coefficientEvaluation =
                    evaluation
                            .getCoefficientEvaluation();

            if (valeur == null
                    || !estStrictementPositif(bareme)
                    || !estStrictementPositif(
                    coefficientEvaluation
            )) {
                continue;
            }

            BigDecimal noteNormalisee =
                    valeur.multiply(NOTE_SUR_VINGT)
                            .divide(
                                    bareme,
                                    ECHELLE_CALCUL,
                                    RoundingMode.HALF_UP
                            );

            sommeNotesPonderees =
                    sommeNotesPonderees.add(
                            noteNormalisee.multiply(
                                    coefficientEvaluation
                            )
                    );

            sommeCoefficientsEvaluations =
                    sommeCoefficientsEvaluations.add(
                            coefficientEvaluation
                    );

            nombreNotes++;
        }

        if (!estStrictementPositif(
                sommeCoefficientsEvaluations
        )) {
            return new ResultatMoyenne(
                    null,
                    null,
                    0
            );
        }

        BigDecimal moyenneBrute =
                sommeNotesPonderees.divide(
                        sommeCoefficientsEvaluations,
                        ECHELLE_CALCUL,
                        RoundingMode.HALF_UP
                );

        BigDecimal moyenneArrondie =
                moyenneBrute.setScale(
                        ECHELLE_AFFICHAGE,
                        RoundingMode.HALF_UP
                );

        return new ResultatMoyenne(
                moyenneBrute,
                moyenneArrondie,
                nombreNotes
        );
    }

    private BigDecimal calculerMoyenneGenerale(
            BigDecimal sommeMoyennesPonderees,
            BigDecimal sommeCoefficientsMatieres
    ) {
        if (!estStrictementPositif(
                sommeCoefficientsMatieres
        )) {
            return null;
        }

        return sommeMoyennesPonderees.divide(
                sommeCoefficientsMatieres,
                ECHELLE_CALCUL,
                RoundingMode.HALF_UP
        ).setScale(
                ECHELLE_AFFICHAGE,
                RoundingMode.HALF_UP
        );
    }

    private boolean concernePeriode(
            Enseignement enseignement,
            Periode periode
    ) {
        if (enseignement == null) {
            return false;
        }

        LocalDate debutEnseignement =
                enseignement.getDateDebut();

        LocalDate finEnseignement =
                enseignement.getDateFin();

        boolean commenceAvantFinPeriode =
                debutEnseignement == null
                        || !debutEnseignement.isAfter(
                        periode.getDateFin()
                );

        boolean termineApresDebutPeriode =
                finEnseignement == null
                        || !finEnseignement.isBefore(
                        periode.getDateDebut()
                );

        return commenceAvantFinPeriode
                && termineApresDebutPeriode;
    }

    private boolean estStrictementPositif(
            BigDecimal valeur
    ) {
        return valeur != null
                && valeur.compareTo(
                BigDecimal.ZERO
        ) > 0;
    }

    private String nomMatiere(
            Enseignement enseignement
    ) {
        if (enseignement == null
                || enseignement.getMatiere() == null
                || enseignement.getMatiere()
                .getNom() == null) {
            return "";
        }

        return enseignement
                .getMatiere()
                .getNom();
    }

    private Scolarite findScolariteById(
            Long idScolarite
    ) {
        return scolariteRepository
                .findById(idScolarite)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La scolarité ayant l'identifiant "
                                        + idScolarite
                                        + " est introuvable."
                        )
                );
    }

    private Periode findPeriodeById(
            Long idPeriode
    ) {
        return periodeRepository
                .findById(idPeriode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La période ayant l'identifiant "
                                        + idPeriode
                                        + " est introuvable."
                        )
                );
    }

    private record ResultatMoyenne(
            BigDecimal moyenneBrute,
            BigDecimal moyenneArrondie,
            int nombreNotes
    ) {
    }
}
