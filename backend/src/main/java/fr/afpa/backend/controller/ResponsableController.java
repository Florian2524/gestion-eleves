package fr.afpa.backend.controller;

import fr.afpa.backend.dto.responsable.ResponsableRequest;
import fr.afpa.backend.dto.responsable.ResponsableResponse;
import fr.afpa.backend.service.ResponsableService;
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
@RequestMapping("/responsables")
public class ResponsableController {

    private final ResponsableService responsableService;

    public ResponsableController(
            ResponsableService responsableService
    ) {
        this.responsableService = responsableService;
    }

    @GetMapping
    public ResponseEntity<List<ResponsableResponse>> findAll() {
        return ResponseEntity.ok(
                responsableService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponsableResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                responsableService.findById(id)
        );
    }

    @PostMapping
    public ResponseEntity<ResponsableResponse> create(
            @Valid @RequestBody ResponsableRequest request
    ) {
        ResponsableResponse response =
                responsableService.create(request);

        URI location = URI.create(
                "/responsables/" + response.idPersonne()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponsableResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ResponsableRequest request
    ) {
        return ResponseEntity.ok(
                responsableService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        responsableService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
