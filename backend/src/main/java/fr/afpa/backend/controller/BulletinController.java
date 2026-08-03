package fr.afpa.backend.controller;

import fr.afpa.backend.dto.bulletin.BulletinDto;
import fr.afpa.backend.service.BulletinCalculService;
import fr.afpa.backend.service.BulletinPdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bulletins")
public class BulletinController {

    private final BulletinCalculService bulletinCalculService;
    private final BulletinPdfService bulletinPdfService;

    public BulletinController(
            BulletinCalculService bulletinCalculService,
            BulletinPdfService bulletinPdfService
    ) {
        this.bulletinCalculService =
                bulletinCalculService;

        this.bulletinPdfService =
                bulletinPdfService;
    }

    @GetMapping(
            "/calcul/{idScolarite}/{idPeriode}"
    )
    public ResponseEntity<BulletinDto> calculer(
            @PathVariable Long idScolarite,
            @PathVariable Long idPeriode
    ) {
        return ResponseEntity.ok(
                bulletinCalculService.calculer(
                        idScolarite,
                        idPeriode
                )
        );
    }

    @GetMapping(
            value = "/calcul/{idScolarite}/{idPeriode}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> telechargerPdf(
            @PathVariable Long idScolarite,
            @PathVariable Long idPeriode
    ) {
        byte[] pdf =
                bulletinPdfService.generer(
                        idScolarite,
                        idPeriode
                );

        String fileName =
                "bulletin-scolarite-"
                        + idScolarite
                        + "-periode-"
                        + idPeriode
                        + ".pdf";

        String contentDisposition =
                ContentDisposition
                        .attachment()
                        .filename(fileName)
                        .build()
                        .toString();

        return ResponseEntity.ok()
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .contentLength(pdf.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition
                )
                .body(pdf);
    }
}
