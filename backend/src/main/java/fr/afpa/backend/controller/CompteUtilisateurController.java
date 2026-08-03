package fr.afpa.backend.controller;

import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurCreateRequest;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurResponse;
import fr.afpa.backend.dto.compteutilisateur.CompteUtilisateurUpdateRequest;
import fr.afpa.backend.service.CompteUtilisateurService;
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
@RequestMapping("/comptes-utilisateurs")
public class CompteUtilisateurController {

    private final CompteUtilisateurService
            compteUtilisateurService;

    public CompteUtilisateurController(
            CompteUtilisateurService compteUtilisateurService
    ) {
        this.compteUtilisateurService =
                compteUtilisateurService;
    }

    @GetMapping
    public ResponseEntity<List<CompteUtilisateurResponse>>
    findAll() {
        return ResponseEntity.ok(
                compteUtilisateurService.findAll()
        );
    }

    @GetMapping("/{idUtilisateur}")
    public ResponseEntity<CompteUtilisateurResponse> findById(
            @PathVariable Long idUtilisateur
    ) {
        return ResponseEntity.ok(
                compteUtilisateurService.findById(
                        idUtilisateur
                )
        );
    }

    @PostMapping
    public ResponseEntity<CompteUtilisateurResponse> create(
            @Valid @RequestBody
            CompteUtilisateurCreateRequest request
    ) {
        CompteUtilisateurResponse response =
                compteUtilisateurService.create(request);

        URI location = URI.create(
                "/comptes-utilisateurs/"
                        + response.idUtilisateur()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{idUtilisateur}")
    public ResponseEntity<CompteUtilisateurResponse> update(
            @PathVariable Long idUtilisateur,
            @Valid @RequestBody
            CompteUtilisateurUpdateRequest request
    ) {
        return ResponseEntity.ok(
                compteUtilisateurService.update(
                        idUtilisateur,
                        request
                )
        );
    }

    @DeleteMapping("/{idUtilisateur}")
    public ResponseEntity<Void> delete(
            @PathVariable Long idUtilisateur
    ) {
        compteUtilisateurService.delete(idUtilisateur);

        return ResponseEntity.noContent().build();
    }
}
