package fr.afpa.backend.controller;

import fr.afpa.backend.dto.enseignant.EnseignantRequest;
import fr.afpa.backend.dto.enseignant.EnseignantResponse;
import fr.afpa.backend.service.EnseignantService;
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
@RequestMapping("/enseignants")
public class EnseignantController {

    private final EnseignantService enseignantService;

    public EnseignantController(
            EnseignantService enseignantService
    ) {
        this.enseignantService = enseignantService;
    }

    @GetMapping
    public ResponseEntity<List<EnseignantResponse>> findAll() {
        return ResponseEntity.ok(
                enseignantService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnseignantResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                enseignantService.findById(id)
        );
    }

    @PostMapping
    public ResponseEntity<EnseignantResponse> create(
            @Valid @RequestBody EnseignantRequest request
    ) {
        EnseignantResponse response =
                enseignantService.create(request);

        URI location = URI.create(
                "/enseignants/" + response.idPersonne()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnseignantResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EnseignantRequest request
    ) {
        return ResponseEntity.ok(
                enseignantService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        enseignantService.delete(id);

        return ResponseEntity.noContent().build();
    }
}