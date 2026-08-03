package fr.afpa.backend.controller;

import fr.afpa.backend.dto.authentication.AuthenticationResponse;
import fr.afpa.backend.dto.authentication.LoginRequest;
import fr.afpa.backend.dto.authentication.RegisterRequest;
import fr.afpa.backend.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService
            authenticationService;

    public AuthenticationController(
            AuthenticationService authenticationService
    ) {
        this.authenticationService =
                authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                authenticationService.login(request)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request,
            Authentication currentAuthentication
    ) {
        AuthenticationResponse response =
                authenticationService.register(
                        request,
                        currentAuthentication
                );

        URI location = URI.create(
                "/comptes-utilisateurs/"
                        + response.idUtilisateur()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }
}
