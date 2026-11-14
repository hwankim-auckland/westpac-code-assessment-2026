package nz.co.westpac.poc.local;

import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/local")
@Profile("local")
public class LocalTokenController {

    private final JwtEncoder jwtEncoder;

    public LocalTokenController(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/token")
    public LocalTokenResponse token(@Valid @RequestBody LocalTokenRequest request) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("local")
                .subject(request.customerId().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(1, ChronoUnit.HOURS))
                .build();
        return new LocalTokenResponse(
                jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue());
    }

    public record LocalTokenRequest(@NotNull UUID customerId) {
    }
    public record LocalTokenResponse(String token) {
    }
}
