package fr.afpa.backend.controller;

import fr.afpa.backend.dto.scolarite.ScolariteRequest;
import fr.afpa.backend.dto.scolarite.ScolariteResponse;
import fr.afpa.backend.service.ScolariteService;
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
@RequestMapping("/scolarites")
public class ScolariteController {

    private final ScolariteService scolariteService;

    public ScolariteController(
            ScolariteService scolariteService
    ) {
        this.scolariteService = scolariteService;
    }

    @GetMapping
    public ResponseEntity<List<ScolariteResponse>> findAll() {
        return ResponseEntity.ok(
                scolariteService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScolariteResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                scolariteService.findById(id)
        );
    }

    @PostMapping
    public ResponseEntity<ScolariteResponse> create(
            @Valid @RequestBody ScolariteRequest request
    ) {
        ScolariteResponse response =
                scolariteService.create(request);

        URI location = URI.create(
                "/scolarites/" + response.idScolarite()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScolariteResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ScolariteRequest request
    ) {
        return ResponseEntity.ok(
                scolariteService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        scolariteService.delete(id);

        return ResponseEntity.noContent().build();
    }
}