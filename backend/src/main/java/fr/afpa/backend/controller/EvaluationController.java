package fr.afpa.backend.controller;

import fr.afpa.backend.dto.evaluation.EvaluationRequest;
import fr.afpa.backend.dto.evaluation.EvaluationResponse;
import fr.afpa.backend.service.EvaluationService;
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
@RequestMapping("/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(
            EvaluationService evaluationService
    ) {
        this.evaluationService = evaluationService;
    }

    @GetMapping
    public ResponseEntity<List<EvaluationResponse>> findAll() {
        return ResponseEntity.ok(
                evaluationService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluationResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                evaluationService.findById(id)
        );
    }

    @PostMapping
    public ResponseEntity<EvaluationResponse> create(
            @Valid @RequestBody EvaluationRequest request
    ) {
        EvaluationResponse response =
                evaluationService.create(request);

        URI location = URI.create(
                "/evaluations/"
                        + response.idEvaluation()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationRequest request
    ) {
        return ResponseEntity.ok(
                evaluationService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        evaluationService.delete(id);

        return ResponseEntity.noContent().build();
    }
}