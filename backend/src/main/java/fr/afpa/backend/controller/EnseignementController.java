package fr.afpa.backend.controller;

import fr.afpa.backend.dto.enseignement.EnseignementRequest;
import fr.afpa.backend.dto.enseignement.EnseignementResponse;
import fr.afpa.backend.service.EnseignementService;
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
@RequestMapping("/enseignements")
public class EnseignementController {

    private final EnseignementService enseignementService;

    public EnseignementController(
            EnseignementService enseignementService
    ) {
        this.enseignementService = enseignementService;
    }

    @GetMapping
    public ResponseEntity<List<EnseignementResponse>> findAll() {
        return ResponseEntity.ok(
                enseignementService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnseignementResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                enseignementService.findById(id)
        );
    }

    @PostMapping
    public ResponseEntity<EnseignementResponse> create(
            @Valid @RequestBody EnseignementRequest request
    ) {
        EnseignementResponse response =
                enseignementService.create(request);

        URI location = URI.create(
                "/enseignements/"
                        + response.idEnseignement()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnseignementResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EnseignementRequest request
    ) {
        return ResponseEntity.ok(
                enseignementService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        enseignementService.delete(id);

        return ResponseEntity.noContent().build();
    }
}