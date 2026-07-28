package fr.afpa.backend.controller;

import fr.afpa.backend.dto.matiere.MatiereRequest;
import fr.afpa.backend.dto.matiere.MatiereResponse;
import fr.afpa.backend.service.MatiereService;
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
@RequestMapping("/matieres")
public class MatiereController {

    private final MatiereService matiereService;

    public MatiereController(MatiereService matiereService) {
        this.matiereService = matiereService;
    }

    @GetMapping
    public ResponseEntity<List<MatiereResponse>> findAll() {
        return ResponseEntity.ok(matiereService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatiereResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(matiereService.findById(id));
    }

    @PostMapping
    public ResponseEntity<MatiereResponse> create(
            @Valid @RequestBody MatiereRequest request
    ) {
        MatiereResponse response = matiereService.create(request);

        URI location = URI.create(
                "/matieres/" + response.idMatiere()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatiereResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MatiereRequest request
    ) {
        return ResponseEntity.ok(
                matiereService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        matiereService.delete(id);

        return ResponseEntity.noContent().build();
    }
}