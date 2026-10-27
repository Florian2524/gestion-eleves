package fr.afpa.backend.controller;

import fr.afpa.backend.dto.personne.PersonneResponse;
import fr.afpa.backend.dto.eleve.*;
import fr.afpa.backend.dto.enseignant.*;
import fr.afpa.backend.dto.responsable.*;
import fr.afpa.backend.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/personnes")
@PreAuthorize("hasRole('ADMIN')")
public class PersonneController {
    private final ProfilPersonneService personnes;
    private final EleveService eleves;
    private final EnseignantService enseignants;
    private final ResponsableService responsables;

    public PersonneController(ProfilPersonneService personnes, EleveService eleves,
            EnseignantService enseignants, ResponsableService responsables) {
        this.personnes = personnes; this.eleves = eleves;
        this.enseignants = enseignants; this.responsables = responsables;
    }

    @GetMapping
    public List<PersonneResponse> findAll() { return personnes.findAll(); }

    @PostMapping("/{idPersonne}/eleve")
    public ResponseEntity<EleveResponse> ajouterEleve(@PathVariable Long idPersonne,
            @Valid @RequestBody EleveProfilRequest request) {
        var response = eleves.ajouterProfil(idPersonne, request);
        return ResponseEntity.created(URI.create("/eleves/" + response.idPersonne())).body(response);
    }

    @PostMapping("/{idPersonne}/enseignant")
    public ResponseEntity<EnseignantResponse> ajouterEnseignant(@PathVariable Long idPersonne,
            @Valid @RequestBody EnseignantProfilRequest request) {
        var response = enseignants.ajouterProfil(idPersonne, request);
        return ResponseEntity.created(URI.create("/enseignants/" + response.idPersonne())).body(response);
    }

    @PostMapping("/{idPersonne}/responsable")
    public ResponseEntity<ResponsableResponse> ajouterResponsable(@PathVariable Long idPersonne,
            @Valid @RequestBody ResponsableProfilRequest request) {
        var response = responsables.ajouterProfil(idPersonne, request);
        return ResponseEntity.created(URI.create("/responsables/" + response.idPersonne())).body(response);
    }
}
