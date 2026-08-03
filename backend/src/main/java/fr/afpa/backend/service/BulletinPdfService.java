package fr.afpa.backend.service;

import fr.afpa.backend.dto.bulletin.BulletinDto;
import fr.afpa.backend.dto.bulletin.LigneBulletinDto;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class BulletinPdfService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Font TITLE_FONT =
            new Font(
                    Font.HELVETICA,
                    18,
                    Font.BOLD
            );

    private static final Font SECTION_FONT =
            new Font(
                    Font.HELVETICA,
                    12,
                    Font.BOLD
            );

    private static final Font LABEL_FONT =
            new Font(
                    Font.HELVETICA,
                    10,
                    Font.BOLD
            );

    private static final Font NORMAL_FONT =
            new Font(
                    Font.HELVETICA,
                    10,
                    Font.NORMAL
            );

    private static final Font SMALL_FONT =
            new Font(
                    Font.HELVETICA,
                    9,
                    Font.NORMAL
            );

    private static final Color HEADER_BACKGROUND =
            new Color(
                    230,
                    230,
                    230
            );

    private final BulletinCalculService bulletinCalculService;

    public BulletinPdfService(
            BulletinCalculService bulletinCalculService
    ) {
        this.bulletinCalculService =
                bulletinCalculService;
    }

    public byte[] generer(
            Long idScolarite,
            Long idPeriode
    ) {
        BulletinDto bulletin =
                bulletinCalculService.calculer(
                        idScolarite,
                        idPeriode
                );

        return genererDocument(bulletin);
    }

    private byte[] genererDocument(
            BulletinDto bulletin
    ) {
        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        Document document =
                new Document(
                        PageSize.A4,
                        36,
                        36,
                        36,
                        36
                );

        try {
            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.addTitle(
                    "Bulletin scolaire - "
                            + texteOuTiret(
                            bulletin.nomEleve()
                    )
                            + " "
                            + texteOuTiret(
                            bulletin.prenomEleve()
                    )
            );

            document.addAuthor(
                    "Application Gestion des élèves"
            );

            document.open();

            ajouterTitre(
                    document,
                    bulletin
            );

            ajouterInformations(
                    document,
                    bulletin
            );

            ajouterLignes(
                    document,
                    bulletin
            );

            ajouterMoyenneGenerale(
                    document,
                    bulletin
            );
        }
        catch (DocumentException exception) {
            throw new IllegalStateException(
                    "La génération du bulletin PDF a échoué.",
                    exception
            );
        }
        finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return outputStream.toByteArray();
    }

    private void ajouterTitre(
            Document document,
            BulletinDto bulletin
    ) throws DocumentException {

        Paragraph title =
                new Paragraph(
                        "Bulletin scolaire",
                        TITLE_FONT
                );

        title.setAlignment(
                Element.ALIGN_CENTER
        );

        title.setSpacingAfter(8);

        document.add(title);

        Paragraph period =
                new Paragraph(
                        construireLibellePeriode(
                                bulletin
                        ),
                        SECTION_FONT
                );

        period.setAlignment(
                Element.ALIGN_CENTER
        );

        period.setSpacingAfter(18);

        document.add(period);
    }

    private void ajouterInformations(
            Document document,
            BulletinDto bulletin
    ) throws DocumentException {

        PdfPTable table =
                new PdfPTable(2);

        table.setWidthPercentage(100);
        table.setWidths(
                new float[]{1, 2}
        );

        ajouterLigneInformation(
                table,
                "Élève",
                construireIdentiteEleve(
                        bulletin
                )
        );

        ajouterLigneInformation(
                table,
                "Matricule",
                bulletin.matriculeEleve()
        );

        ajouterLigneInformation(
                table,
                "Classe",
                bulletin.nomClasse()
        );

        ajouterLigneInformation(
                table,
                "Niveau",
                bulletin.niveauClasse()
        );

        ajouterLigneInformation(
                table,
                "Année scolaire",
                bulletin.anneeScolaire()
        );

        table.setSpacingAfter(18);

        document.add(table);
    }

    private void ajouterLigneInformation(
            PdfPTable table,
            String label,
            String value
    ) {
        PdfPCell labelCell =
                creerCellule(
                        label,
                        LABEL_FONT,
                        Element.ALIGN_LEFT
                );

        labelCell.setBackgroundColor(
                HEADER_BACKGROUND
        );

        table.addCell(labelCell);

        table.addCell(
                creerCellule(
                        texteOuTiret(value),
                        NORMAL_FONT,
                        Element.ALIGN_LEFT
                )
        );
    }

    private void ajouterLignes(
            Document document,
            BulletinDto bulletin
    ) throws DocumentException {

        Paragraph sectionTitle =
                new Paragraph(
                        "Résultats par matière",
                        SECTION_FONT
                );

        sectionTitle.setSpacingAfter(8);

        document.add(sectionTitle);

        PdfPTable table =
                new PdfPTable(5);

        table.setWidthPercentage(100);

        table.setWidths(
                new float[]{
                        3.2f,
                        1.2f,
                        1.1f,
                        1.1f,
                        1.6f
                }
        );

        ajouterEnteteTableau(
                table,
                "Matière"
        );

        ajouterEnteteTableau(
                table,
                "Code"
        );

        ajouterEnteteTableau(
                table,
                "Coef."
        );

        ajouterEnteteTableau(
                table,
                "Notes"
        );

        ajouterEnteteTableau(
                table,
                "Moyenne /20"
        );

        table.setHeaderRows(1);

        if (bulletin.lignes().isEmpty()) {
            PdfPCell emptyCell =
                    creerCellule(
                            "Aucun résultat disponible pour cette période.",
                            NORMAL_FONT,
                            Element.ALIGN_CENTER
                    );

            emptyCell.setColspan(5);
            emptyCell.setPadding(12);

            table.addCell(emptyCell);
        }
        else {
            for (LigneBulletinDto ligne
                    : bulletin.lignes()) {

                table.addCell(
                        creerCellule(
                                texteOuTiret(
                                        ligne.nomMatiere()
                                ),
                                SMALL_FONT,
                                Element.ALIGN_LEFT
                        )
                );

                table.addCell(
                        creerCellule(
                                texteOuTiret(
                                        ligne.codeMatiere()
                                ),
                                SMALL_FONT,
                                Element.ALIGN_CENTER
                        )
                );

                table.addCell(
                        creerCellule(
                                formaterDecimal(
                                        ligne.coefficientMatiere()
                                ),
                                SMALL_FONT,
                                Element.ALIGN_CENTER
                        )
                );

                table.addCell(
                        creerCellule(
                                String.valueOf(
                                        ligne.nombreNotes()
                                ),
                                SMALL_FONT,
                                Element.ALIGN_CENTER
                        )
                );

                table.addCell(
                        creerCellule(
                                formaterDecimal(
                                        ligne.moyenneSur20()
                                ),
                                SMALL_FONT,
                                Element.ALIGN_CENTER
                        )
                );
            }
        }

        table.setSpacingAfter(18);

        document.add(table);
    }

    private void ajouterEnteteTableau(
            PdfPTable table,
            String text
    ) {
        PdfPCell cell =
                creerCellule(
                        text,
                        LABEL_FONT,
                        Element.ALIGN_CENTER
                );

        cell.setBackgroundColor(
                HEADER_BACKGROUND
        );

        table.addCell(cell);
    }

    private void ajouterMoyenneGenerale(
            Document document,
            BulletinDto bulletin
    ) throws DocumentException {

        String moyenneText =
                bulletin.moyenneGenerale() == null
                        ? "Moyenne générale : non calculable"
                        : "Moyenne générale : "
                        + formaterDecimal(
                        bulletin.moyenneGenerale()
                )
                        + " / 20";

        Paragraph moyenne =
                new Paragraph(
                        moyenneText,
                        SECTION_FONT
                );

        moyenne.setAlignment(
                Element.ALIGN_RIGHT
        );

        document.add(moyenne);
    }

    private PdfPCell creerCellule(
            String text,
            Font font,
            int alignment
    ) {
        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                font
                        )
                );

        cell.setHorizontalAlignment(
                alignment
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(6);

        return cell;
    }

    private String construireIdentiteEleve(
            BulletinDto bulletin
    ) {
        return (
                texteOuTiret(
                        bulletin.prenomEleve()
                )
                        + " "
                        + texteOuTiret(
                        bulletin.nomEleve()
                )
        ).trim();
    }

    private String construireLibellePeriode(
            BulletinDto bulletin
    ) {
        String libelle =
                texteOuTiret(
                        bulletin.libellePeriode()
                );

        LocalDate dateDebut =
                bulletin.dateDebutPeriode();

        LocalDate dateFin =
                bulletin.dateFinPeriode();

        if (dateDebut == null
                || dateFin == null) {
            return libelle;
        }

        return libelle
                + " — du "
                + dateDebut.format(
                DATE_FORMATTER
        )
                + " au "
                + dateFin.format(
                DATE_FORMATTER
        );
    }

    private String formaterDecimal(
            BigDecimal value
    ) {
        if (value == null) {
            return "-";
        }

        return value
                .stripTrailingZeros()
                .toPlainString();
    }

    private String texteOuTiret(
            String value
    ) {
        if (value == null
                || value.isBlank()) {
            return "-";
        }

        return value;
    }
}
