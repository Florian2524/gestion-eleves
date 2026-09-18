package fr.afpa.backend.controller;

import fr.afpa.backend.dto.eleve.EleveRequest;
import fr.afpa.backend.dto.eleve.EleveResponse;
import fr.afpa.backend.service.EleveService;
import fr.afpa.backend.service.ElevePhotoService;
import fr.afpa.backend.security.SecurityExpressions;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
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
@RequestMapping("/eleves")
public class EleveController {

    private final EleveService eleveService;
    private final SecurityExpressions securityExpressions;
    private final ElevePhotoService photoService;

    public EleveController(EleveService eleveService, SecurityExpressions securityExpressions,
                           ElevePhotoService photoService) {
        this.eleveService = eleveService;
        this.securityExpressions = securityExpressions;
        this.photoService = photoService;
    }

    @GetMapping
    public ResponseEntity<List<EleveResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(eleveService.findAll().stream()
                .filter(item -> securityExpressions.peutConsulterEleve(item.idPersonne(), authentication))
                .toList());
    }

    @GetMapping("/{id}")
    @PostAuthorize("@securityExpressions.peutConsulterEleve(returnObject.body.idPersonne(), authentication)")
    public ResponseEntity<EleveResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eleveService.findById(id));
    }

    @GetMapping("/{id}/photo")
    @PreAuthorize("@securityExpressions.peutConsulterEleve(#id, authentication)")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        ElevePhotoService.Photo photo = photoService.read(id);
        return ResponseEntity.ok().contentType(photo.mediaType())
                .cacheControl(CacheControl.noStore()).body(photo.bytes());
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EleveResponse> uploadPhoto(@PathVariable Long id,
                                                      @RequestParam("file") MultipartFile file) {
        photoService.upload(id, file);
        return ResponseEntity.ok(eleveService.findById(id));
    }

    @DeleteMapping("/{id}/photo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
        photoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<EleveResponse> create(
            @Valid @RequestBody EleveRequest request
    ) {
        EleveResponse response = eleveService.create(request);

        URI location = URI.create(
                "/eleves/" + response.idPersonne()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EleveResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EleveRequest request
    ) {
        return ResponseEntity.ok(
                eleveService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        eleveService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
