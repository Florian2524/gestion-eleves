package fr.afpa.backend.controller;

import fr.afpa.backend.dto.inscription.InscriptionRequest;
import fr.afpa.backend.dto.inscription.InscriptionResponse;
import fr.afpa.backend.service.InscriptionService;
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
@RequestMapping("/inscriptions")
public class InscriptionController {

    private final InscriptionService inscriptionService;

    public InscriptionController(
            InscriptionService inscriptionService
    ) {
        this.inscriptionService = inscriptionService;
    }

    @GetMapping
    public ResponseEntity<List<InscriptionResponse>> findAll() {
        return ResponseEntity.ok(
                inscriptionService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<InscriptionResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                inscriptionService.findById(id)
        );
    }

    @PostMapping
    public ResponseEntity<InscriptionResponse> create(
            @Valid @RequestBody InscriptionRequest request
    ) {
        InscriptionResponse response =
                inscriptionService.create(request);

        URI location = URI.create(
                "/inscriptions/" + response.idInscription()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InscriptionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InscriptionRequest request
    ) {
        return ResponseEntity.ok(
                inscriptionService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        inscriptionService.delete(id);

        return ResponseEntity.noContent().build();
    }
}