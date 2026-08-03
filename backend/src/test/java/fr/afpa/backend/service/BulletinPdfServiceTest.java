package fr.afpa.backend.service;

import fr.afpa.backend.dto.bulletin.BulletinDto;
import fr.afpa.backend.dto.bulletin.LigneBulletinDto;
import fr.afpa.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openpdf.text.pdf.PdfReader;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulletinPdfServiceTest {

    private static final Long ID_SCOLARITE = 10L;
    private static final Long ID_PERIODE = 20L;

    @Mock
    private BulletinCalculService bulletinCalculService;

    private BulletinPdfService bulletinPdfService;

    private BulletinDto bulletin;

    @BeforeEach
    void setUp() {
        bulletinPdfService =
                new BulletinPdfService(
                        bulletinCalculService
                );

        LigneBulletinDto ligne =
                new LigneBulletinDto(
                        100L,
                        1000L,
                        "MATH",
                        "Mathématiques",
                        BigDecimal.valueOf(4),
                        2,
                        BigDecimal.valueOf(15.33)
                );

        bulletin = new BulletinDto(
                ID_SCOLARITE,
                ID_PERIODE,
                40L,
                "ELV-001",
                "Martin",
                "Léa",
                30L,
                "6A",
                "Sixième",
                "2026-2027",
                "Trimestre 1",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 20),
                List.of(ligne),
                BigDecimal.valueOf(15.33)
        );
    }

    @Test
    void shouldGenerateReadablePdf()
            throws Exception {

        when(bulletinCalculService.calculer(
                ID_SCOLARITE,
                ID_PERIODE
        )).thenReturn(bulletin);

        byte[] result =
                bulletinPdfService.generer(
                        ID_SCOLARITE,
                        ID_PERIODE
                );

        assertPdfIsReadable(result);

        verify(bulletinCalculService).calculer(
                ID_SCOLARITE,
                ID_PERIODE
        );
    }

    @Test
    void shouldGeneratePdfWithoutLinesOrAverage()
            throws Exception {

        BulletinDto emptyBulletin =
                new BulletinDto(
                        ID_SCOLARITE,
                        ID_PERIODE,
                        40L,
                        "ELV-001",
                        "Martin",
                        "Léa",
                        30L,
                        "6A",
                        "Sixième",
                        "2026-2027",
                        "Trimestre 1",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 12, 20),
                        List.of(),
                        null
                );

        when(bulletinCalculService.calculer(
                ID_SCOLARITE,
                ID_PERIODE
        )).thenReturn(emptyBulletin);

        byte[] result =
                bulletinPdfService.generer(
                        ID_SCOLARITE,
                        ID_PERIODE
                );

        assertPdfIsReadable(result);

        verify(bulletinCalculService).calculer(
                ID_SCOLARITE,
                ID_PERIODE
        );
    }

    @Test
    void shouldPropagateNotFoundException() {
        ResourceNotFoundException exception =
                new ResourceNotFoundException(
                        "La scolarité ayant l'identifiant "
                                + ID_SCOLARITE
                                + " est introuvable."
                );

        when(bulletinCalculService.calculer(
                ID_SCOLARITE,
                ID_PERIODE
        )).thenThrow(exception);

        assertThatThrownBy(() ->
                bulletinPdfService.generer(
                        ID_SCOLARITE,
                        ID_PERIODE
                )
        )
                .isSameAs(exception);

        verify(bulletinCalculService).calculer(
                ID_SCOLARITE,
                ID_PERIODE
        );
    }

    private void assertPdfIsReadable(
            byte[] pdf
    ) throws Exception {

        assertThat(pdf)
                .isNotNull()
                .isNotEmpty();

        assertThat(pdf.length)
                .isGreaterThan(500);

        String signature =
                new String(
                        pdf,
                        0,
                        5,
                        StandardCharsets.US_ASCII
                );

        assertThat(signature)
                .isEqualTo("%PDF-");

        PdfReader reader =
                new PdfReader(pdf);

        try {
            assertThat(
                    reader.getNumberOfPages()
            ).isGreaterThanOrEqualTo(1);
        }
        finally {
            reader.close();
        }
    }
}
