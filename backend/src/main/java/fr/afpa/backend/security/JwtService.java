package fr.afpa.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${application.security.jwt.expiration-seconds}")
            long expirationSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(
            CompteUtilisateurPrincipal principal
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(
                expirationSeconds
        );

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("gestion-eleves-api")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(principal.getUsername())
                .claim(
                        "roles",
                        List.of(principal.getRole().name())
                )
                .claim(
                        "idUtilisateur",
                        principal.getIdUtilisateur()
                )
                .claim(
                        "idPersonne",
                        principal.getIdPersonne()
                )
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        header,
                        claims
                )
        ).getTokenValue();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
