package fr.afpa.backend.controller;

import fr.afpa.backend.dto.periode.PeriodeRequest;
import fr.afpa.backend.dto.periode.PeriodeResponse;
import fr.afpa.backend.service.PeriodeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/periodes")
public class PeriodeController {

    private final PeriodeService periodeService;

    public PeriodeController(PeriodeService periodeService) {
        this.periodeService = periodeService;
    }

    @GetMapping
    public ResponseEntity<List<PeriodeResponse>> findAll() {
        return ResponseEntity.ok(periodeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeriodeResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                periodeService.findById(id)
        );
    }

    @PostMapping
    public ResponseEntity<PeriodeResponse> create(
            @Valid @RequestBody PeriodeRequest request
    ) {
        PeriodeResponse response =
                periodeService.create(request);

        URI location = URI.create(
                "/periodes/" + response.idPeriode()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PeriodeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PeriodeRequest request
    ) {
        return ResponseEntity.ok(
                periodeService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        periodeService.delete(id);

        return ResponseEntity.noContent().build();
    }
}