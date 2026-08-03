package fr.afpa.backend.controller;

import fr.afpa.backend.dto.bulletin.BulletinDto;
import fr.afpa.backend.service.BulletinCalculService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bulletins")
public class BulletinController {

    private final BulletinCalculService bulletinCalculService;

    public BulletinController(
            BulletinCalculService bulletinCalculService
    ) {
        this.bulletinCalculService =
                bulletinCalculService;
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
}
