package fr.afpa.backend.controller;

import fr.afpa.backend.dto.ponderer.*;
import fr.afpa.backend.service.PondererService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/ponderations")
public class PondererController {
    private final PondererService service;
    public PondererController(PondererService service) { this.service = service; }
    @GetMapping
    public List<PondererResponse> findAll() { return service.findAll(); }
    @GetMapping("/{idClasse}/{idMatiere}")
    public PondererResponse findById(@PathVariable Long idClasse, @PathVariable Long idMatiere) {
        return service.findById(idClasse, idMatiere);
    }
    @PostMapping
    public ResponseEntity<PondererResponse> create(@Valid @RequestBody PondererRequest request) {
        PondererResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/ponderations/" + response.idClasse() + "/" + response.idMatiere())).body(response);
    }
    @PutMapping("/{idClasse}/{idMatiere}")
    public PondererResponse update(@PathVariable Long idClasse, @PathVariable Long idMatiere,
                                  @Valid @RequestBody PondererRequest request) {
        return service.update(idClasse, idMatiere, request);
    }
    @DeleteMapping("/{idClasse}/{idMatiere}")
    public ResponseEntity<Void> delete(@PathVariable Long idClasse, @PathVariable Long idMatiere) {
        service.delete(idClasse, idMatiere);
        return ResponseEntity.noContent().build();
    }
}
