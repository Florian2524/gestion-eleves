package fr.afpa.backend.controller;

import fr.afpa.backend.dto.responsabilitelegale.ResponsabiliteLegaleRequest;
import fr.afpa.backend.dto.responsabilitelegale.ResponsabiliteLegaleResponse;
import fr.afpa.backend.service.ResponsabiliteLegaleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/responsabilites-legales")
public class ResponsabiliteLegaleController {

    private final ResponsabiliteLegaleService
            responsabiliteLegaleService;

    public ResponsabiliteLegaleController(
            ResponsabiliteLegaleService responsabiliteLegaleService
    ) {
        this.responsabiliteLegaleService =
                responsabiliteLegaleService;
    }

    @GetMapping
    public ResponseEntity<List<ResponsabiliteLegaleResponse>>
    findAll() {
        return ResponseEntity.ok(
                responsabiliteLegaleService.findAll()
        );
    }

    @GetMapping("/{idResponsable}/{idEleve}")
    public ResponseEntity<ResponsabiliteLegaleResponse> findById(
            @PathVariable Long idResponsable,
            @PathVariable Long idEleve
    ) {
        return ResponseEntity.ok(
                responsabiliteLegaleService.findById(
                        idResponsable,
                        idEleve
                )
        );
    }

    @PostMapping
    public ResponseEntity<ResponsabiliteLegaleResponse> create(
            @Valid @RequestBody
            ResponsabiliteLegaleRequest request
    ) {
        ResponsabiliteLegaleResponse response =
                responsabiliteLegaleService.create(request);

        URI location = URI.create(
                "/responsabilites-legales/"
                        + response.idResponsable()
                        + "/"
                        + response.idEleve()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @DeleteMapping("/{idResponsable}/{idEleve}")
    public ResponseEntity<Void> delete(
            @PathVariable Long idResponsable,
            @PathVariable Long idEleve
    ) {
        responsabiliteLegaleService.delete(
                idResponsable,
                idEleve
        );

        return ResponseEntity.noContent().build();
    }
}
