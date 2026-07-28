package fr.afpa.backend.controller;

import fr.afpa.backend.dto.eleve.EleveRequest;
import fr.afpa.backend.dto.eleve.EleveResponse;
import fr.afpa.backend.service.EleveService;
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
@RequestMapping("/eleves")
public class EleveController {

    private final EleveService eleveService;

    public EleveController(EleveService eleveService) {
        this.eleveService = eleveService;
    }

    @GetMapping
    public ResponseEntity<List<EleveResponse>> findAll() {
        return ResponseEntity.ok(eleveService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EleveResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eleveService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EleveResponse> create(
            @Valid @RequestBody EleveRequest request
    ) {
        EleveResponse response = eleveService.create(request);

        URI location = URI.create(
                "/eleves/" + response.idPersonne()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EleveResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EleveRequest request
    ) {
        return ResponseEntity.ok(
                eleveService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        eleveService.delete(id);

        return ResponseEntity.noContent().build();
    }
}