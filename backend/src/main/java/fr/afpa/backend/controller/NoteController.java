package fr.afpa.backend.controller;

import fr.afpa.backend.dto.note.NoteRequest;
import fr.afpa.backend.dto.note.NoteResponse;
import fr.afpa.backend.service.NoteService;
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

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> findAll() {
        return ResponseEntity.ok(
                noteService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                noteService.findById(id)
        );
    }

    @PostMapping
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
    public ResponseEntity<NoteResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NoteRequest request
    ) {
        return ResponseEntity.ok(
                noteService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        noteService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
