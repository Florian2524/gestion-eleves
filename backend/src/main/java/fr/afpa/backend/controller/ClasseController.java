package fr.afpa.backend.controller;

import fr.afpa.backend.dto.classe.ClasseRequest;
import fr.afpa.backend.dto.classe.ClasseResponse;
import fr.afpa.backend.service.ClasseService;
import fr.afpa.backend.security.SecurityExpressions;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;
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
@RequestMapping("/classes")
public class ClasseController {

    private final ClasseService classeService;
    private final SecurityExpressions securityExpressions;

    public ClasseController(ClasseService classeService, SecurityExpressions securityExpressions) {
        this.classeService = classeService;
        this.securityExpressions = securityExpressions;
    }

    @GetMapping
    public ResponseEntity<List<ClasseResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(classeService.findAll().stream()
                .filter(item -> securityExpressions.peutConsulterClasse(item.idClasse(), authentication))
                .toList());
    }

    @GetMapping("/{id}")
    @PostAuthorize("@securityExpressions.peutConsulterClasse(returnObject.body.idClasse(), authentication)")
    public ResponseEntity<ClasseResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(classeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ClasseResponse> create(
            @Valid @RequestBody ClasseRequest request
    ) {
        ClasseResponse response = classeService.create(request);

        URI location = URI.create(
                "/classes/" + response.idClasse()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClasseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ClasseRequest request
    ) {
        return ResponseEntity.ok(
                classeService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        classeService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
