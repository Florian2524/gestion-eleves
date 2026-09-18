package fr.afpa.backend.controller;

import fr.afpa.backend.dto.note.NoteRequest;
import fr.afpa.backend.dto.note.NoteResponse;
import fr.afpa.backend.service.NoteService;
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
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;
    private final SecurityExpressions securityExpressions;

    public NoteController(NoteService noteService, SecurityExpressions securityExpressions) {
        this.noteService = noteService;
        this.securityExpressions = securityExpressions;
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(
                noteService.findAll().stream()
                        .filter(item -> securityExpressions.peutConsulterNote(item.idNote(), authentication))
                        .toList()
        );
    }

    @GetMapping("/{id}")
    @PostAuthorize("@securityExpressions.peutConsulterNote(returnObject.body.idNote(), authentication)")
    public ResponseEntity<NoteResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                noteService.findById(id)
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @securityExpressions.estProprietaireEvaluation(#request.idEvaluation(), authentication)")
    public ResponseEntity<NoteResponse> create(
            @Valid @RequestBody NoteRequest request
    ) {
        NoteResponse response =
                noteService.create(request);

        URI location = URI.create(
                "/notes/" + response.idNote()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (@securityExpressions.estProprietaireOuAbsenteNote(#id, authentication) and @securityExpressions.estProprietaireEvaluation(#request.idEvaluation(), authentication))")
    public ResponseEntity<NoteResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NoteRequest request
    ) {
        return ResponseEntity.ok(
                noteService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityExpressions.estProprietaireOuAbsenteNote(#id, authentication)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        noteService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
