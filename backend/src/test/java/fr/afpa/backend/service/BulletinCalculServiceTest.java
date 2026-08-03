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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulletinCalculServiceTest {

    private static final Long ID_SCOLARITE = 10L;
    private static final Long ID_PERIODE = 20L;
    private static final Long ID_CLASSE = 30L;

    @Mock
    private ScolariteRepository scolariteRepository;

    @Mock
    private PeriodeRepository periodeRepository;

    @Mock
    private EnseignementRepository enseignementRepository;

    @Mock
    private NoteRepository noteRepository;

    private BulletinCalculService bulletinCalculService;

    @BeforeEach
    void setUp() {
        bulletinCalculService =
                new BulletinCalculService(
                        scolariteRepository,
                        periodeRepository,
                        enseignementRepository,
                        noteRepository
                );
    }

    @Test
    void shouldRejectCalculationWhenScolariteDoesNotExist() {
        when(scolariteRepository.findById(
                ID_SCOLARITE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                bulletinCalculService.calculer(
                        ID_SCOLARITE,
                        ID_PERIODE
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage(
                        "La scolarité ayant l'identifiant "
                                + ID_SCOLARITE
                                + " est introuvable."
                );

        verify(scolariteRepository)
                .findById(ID_SCOLARITE);

        verifyNoInteractions(
                periodeRepository,
                enseignementRepository,
                noteRepository
        );
    }

    @Test
    void shouldRejectCalculationWhenPeriodDoesNotExist() {
        Scolarite scolarite =
                mock(Scolarite.class);

        when(scolariteRepository.findById(
                ID_SCOLARITE
        )).thenReturn(Optional.of(scolarite));

        when(periodeRepository.findById(
                ID_PERIODE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                bulletinCalculService.calculer(
                        ID_SCOLARITE,
                        ID_PERIODE
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessage(
                        "La période ayant l'identifiant "
                                + ID_PERIODE
                                + " est introuvable."
                );

        verify(scolariteRepository)
                .findById(ID_SCOLARITE);

        verify(periodeRepository)
                .findById(ID_PERIODE);

        verifyNoInteractions(
                enseignementRepository,
                noteRepository
        );
    }

    @Test
    void shouldCalculateWeightedBulletinWithSeveralSubjects() {
        preparerContexte();

        Enseignement mathematiques =
                creerEnseignement(
                        100L,
                        1000L,
                        "MATH",
                        "Mathématiques",
                        BigDecimal.valueOf(4),
                        LocalDate.of(2026, 9, 1),
                        null
                );

        Enseignement francais =
                creerEnseignement(
                        200L,
                        2000L,
                        "FRA",
                        "Français",
                        BigDecimal.valueOf(2),
                        LocalDate.of(2026, 9, 1),
                        null
                );

        Note noteMaths1 =
                creerNote(
                        mathematiques,
                        BigDecimal.valueOf(15),
                        BigDecimal.valueOf(2),
                        BigDecimal.valueOf(20)
                );

        Note noteMaths2 =
                creerNote(
                        mathematiques,
                        BigDecimal.valueOf(12),
                        BigDecimal.ONE,
                        BigDecimal.valueOf(15)
                );

        Note noteFrancais =
                creerNote(
                        francais,
                        BigDecimal.valueOf(14),
                        BigDecimal.ONE,
                        BigDecimal.valueOf(20)
                );

        when(enseignementRepository
                .findAllByClasse_IdClasse(
                        ID_CLASSE
                ))
                .thenReturn(
                        List.of(
                                mathematiques,
                                francais
                        )
                );

        when(noteRepository
                .findAllByScolarite_IdScolariteAndEvaluation_Periode_IdPeriode(
                        ID_SCOLARITE,
                        ID_PERIODE
                ))
                .thenReturn(
                        List.of(
                                noteMaths1,
                                noteMaths2,
                                noteFrancais
                        )
                );

        BulletinDto result =
                bulletinCalculService.calculer(
                        ID_SCOLARITE,
                        ID_PERIODE
                );

        assertThat(result.idScolarite())
                .isEqualTo(ID_SCOLARITE);

        assertThat(result.idPeriode())
                .isEqualTo(ID_PERIODE);

        assertThat(result.nomEleve())
                .isEqualTo("Martin");

        assertThat(result.prenomEleve())
                .isEqualTo("Léa");

        assertThat(result.nomClasse())
                .isEqualTo("6A");

        assertThat(result.libellePeriode())
                .isEqualTo("Trimestre 1");

        assertThat(result.lignes())
                .hasSize(2);

        LigneBulletinDto ligneMaths =
                trouverLigne(
                        result,
                        100L
                );

        assertThat(ligneMaths.nombreNotes())
                .isEqualTo(2);

        assertThat(ligneMaths.moyenneSur20())
                .isEqualByComparingTo("15.33");

        LigneBulletinDto ligneFrancais =
                trouverLigne(
                        result,
                        200L
                );

        assertThat(ligneFrancais.nombreNotes())
                .isEqualTo(1);

        assertThat(ligneFrancais.moyenneSur20())
                .isEqualByComparingTo("14.00");

        assertThat(result.moyenneGenerale())
                .isEqualByComparingTo("14.89");

        verify(noteRepository)
                .findAllByScolarite_IdScolariteAndEvaluation_Periode_IdPeriode(
                        ID_SCOLARITE,
                        ID_PERIODE
                );
    }

    @Test
    void shouldReturnSubjectWithoutAverageWhenItHasNoNote() {
        preparerContexte();

        Enseignement histoire =
                creerEnseignement(
                        300L,
                        3000L,
                        "HIST",
                        "Histoire",
                        BigDecimal.valueOf(3),
                        LocalDate.of(2026, 9, 1),
                        null
                );

        when(enseignementRepository
                .findAllByClasse_IdClasse(
                        ID_CLASSE
                ))
                .thenReturn(List.of(histoire));

        when(noteRepository
                .findAllByScolarite_IdScolariteAndEvaluation_Periode_IdPeriode(
                        ID_SCOLARITE,
                        ID_PERIODE
                ))
                .thenReturn(List.of());

        BulletinDto result =
                bulletinCalculService.calculer(
                        ID_SCOLARITE,
                        ID_PERIODE
                );

        LigneBulletinDto ligne =
                trouverLigne(
                        result,
                        300L
                );

        assertThat(ligne.nombreNotes())
                .isZero();

        assertThat(ligne.moyenneSur20())
                .isNull();

        assertThat(result.moyenneGenerale())
                .isNull();
    }

    @Test
    void shouldIgnoreInvalidBaremeAndEvaluationCoefficient() {
        preparerContexte();

        Enseignement sciences =
                creerEnseignement(
                        400L,
                        4000L,
                        "SCI",
                        "Sciences",
                        BigDecimal.valueOf(2),
                        LocalDate.of(2026, 9, 1),
                        null
                );

        Note noteAvecBaremeNul =
                creerNote(
                        sciences,
                        BigDecimal.valueOf(10),
                        BigDecimal.ONE,
                        BigDecimal.ZERO
                );

        Note noteAvecCoefficientNul =
                creerNote(
                        sciences,
                        BigDecimal.valueOf(10),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(20)
                );

        when(enseignementRepository
                .findAllByClasse_IdClasse(
                        ID_CLASSE
                ))
                .thenReturn(List.of(sciences));

        when(noteRepository
                .findAllByScolarite_IdScolariteAndEvaluation_Periode_IdPeriode(
                        ID_SCOLARITE,
                        ID_PERIODE
                ))
                .thenReturn(
                        List.of(
                                noteAvecBaremeNul,
                                noteAvecCoefficientNul
                        )
                );

        BulletinDto result =
                bulletinCalculService.calculer(
                        ID_SCOLARITE,
                        ID_PERIODE
                );

        LigneBulletinDto ligne =
                trouverLigne(
                        result,
                        400L
                );

        assertThat(ligne.nombreNotes())
                .isZero();

        assertThat(ligne.moyenneSur20())
                .isNull();

        assertThat(result.moyenneGenerale())
                .isNull();
    }

    @Test
    void shouldRoundAverageUsingHalfUp() {
        preparerContexte();

        Enseignement musique =
                creerEnseignement(
                        500L,
                        5000L,
                        "MUS",
                        "Musique",
                        BigDecimal.ONE,
                        LocalDate.of(2026, 9, 1),
                        null
                );

        Note note =
                creerNote(
                        musique,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        BigDecimal.valueOf(3)
                );

        when(enseignementRepository
                .findAllByClasse_IdClasse(
                        ID_CLASSE
                ))
                .thenReturn(List.of(musique));

        when(noteRepository
                .findAllByScolarite_IdScolariteAndEvaluation_Periode_IdPeriode(
                        ID_SCOLARITE,
                        ID_PERIODE
                ))
                .thenReturn(List.of(note));

        BulletinDto result =
                bulletinCalculService.calculer(
                        ID_SCOLARITE,
                        ID_PERIODE
                );

        assertThat(
                trouverLigne(
                        result,
                        500L
                ).moyenneSur20()
        ).isEqualByComparingTo("6.67");

        assertThat(result.moyenneGenerale())
                .isEqualByComparingTo("6.67");
    }

    @Test
    void shouldIgnoreTeachingOutsideRequestedPeriod() {
        preparerContexte();

        Enseignement ancienEnseignement =
                creerEnseignement(
                        600L,
                        6000L,
                        "OLD",
                        "Ancienne matière",
                        BigDecimal.ONE,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 8, 31)
                );

        when(enseignementRepository
                .findAllByClasse_IdClasse(
                        ID_CLASSE
                ))
                .thenReturn(
                        List.of(ancienEnseignement)
                );

        when(noteRepository
                .findAllByScolarite_IdScolariteAndEvaluation_Periode_IdPeriode(
                        ID_SCOLARITE,
                        ID_PERIODE
                ))
                .thenReturn(List.of());

        BulletinDto result =
                bulletinCalculService.calculer(
                        ID_SCOLARITE,
                        ID_PERIODE
                );

        assertThat(result.lignes())
                .isEmpty();

        assertThat(result.moyenneGenerale())
                .isNull();
    }

    private void preparerContexte() {
        Scolarite scolarite =
                mock(Scolarite.class);

        Periode periode =
                mock(Periode.class);

        Eleve eleve =
                mock(Eleve.class);

        Classe classe =
                mock(Classe.class);

        when(scolariteRepository.findById(
                ID_SCOLARITE
        )).thenReturn(Optional.of(scolarite));

        when(periodeRepository.findById(
                ID_PERIODE
        )).thenReturn(Optional.of(periode));

        when(scolarite.getEleve())
                .thenReturn(eleve);

        when(scolarite.getClasse())
                .thenReturn(classe);

        when(eleve.getIdPersonne())
                .thenReturn(40L);

        when(eleve.getMatricule())
                .thenReturn("ELV-001");

        when(eleve.getNom())
                .thenReturn("Martin");

        when(eleve.getPrenom())
                .thenReturn("Léa");

        when(classe.getIdClasse())
                .thenReturn(ID_CLASSE);

        when(classe.getNom())
                .thenReturn("6A");

        when(classe.getNiveau())
                .thenReturn("Sixième");

        when(classe.getAnneeScolaire())
                .thenReturn("2026-2027");

        when(periode.getLibelle())
                .thenReturn("Trimestre 1");

        when(periode.getDateDebut())
                .thenReturn(
                        LocalDate.of(2026, 9, 1)
                );

        when(periode.getDateFin())
                .thenReturn(
                        LocalDate.of(2026, 12, 20)
                );
    }

    private Enseignement creerEnseignement(
            Long idEnseignement,
            Long idMatiere,
            String codeMatiere,
            String nomMatiere,
            BigDecimal coefficientMatiere,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        Enseignement enseignement =
                mock(Enseignement.class);

        Matiere matiere =
                mock(Matiere.class);

        lenient().when(
                enseignement.getIdEnseignement()
        ).thenReturn(idEnseignement);

        lenient().when(
                enseignement.getMatiere()
        ).thenReturn(matiere);

        lenient().when(
                enseignement.getCoefficientMatiere()
        ).thenReturn(coefficientMatiere);

        lenient().when(
                enseignement.getDateDebut()
        ).thenReturn(dateDebut);

        lenient().when(
                enseignement.getDateFin()
        ).thenReturn(dateFin);

        lenient().when(
                matiere.getIdMatiere()
        ).thenReturn(idMatiere);

        lenient().when(
                matiere.getCode()
        ).thenReturn(codeMatiere);

        lenient().when(
                matiere.getNom()
        ).thenReturn(nomMatiere);

        return enseignement;
    }

    private Note creerNote(
            Enseignement enseignement,
            BigDecimal valeur,
            BigDecimal coefficientEvaluation,
            BigDecimal bareme
    ) {
        Note note = mock(Note.class);
        Evaluation evaluation =
                mock(Evaluation.class);

        when(note.getEvaluation())
                .thenReturn(evaluation);

        when(note.getValeur())
                .thenReturn(valeur);

        when(evaluation.getEnseignement())
                .thenReturn(enseignement);

        when(evaluation.getCoefficientEvaluation())
                .thenReturn(coefficientEvaluation);

        when(evaluation.getBareme())
                .thenReturn(bareme);

        return note;
    }

    private LigneBulletinDto trouverLigne(
            BulletinDto bulletin,
            Long idEnseignement
    ) {
        return bulletin.lignes()
                .stream()
                .filter(ligne ->
                        idEnseignement.equals(
                                ligne.idEnseignement()
                        )
                )
                .findFirst()
                .orElseThrow();
    }
}
