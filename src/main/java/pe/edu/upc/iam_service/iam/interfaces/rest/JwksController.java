package pe.edu.upc.iam_service.iam.interfaces.rest;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.iam_service.iam.infrastructure.tokens.jwt.services.JwtKeyProvider;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "JWKS", description = "JWKS Endpoints")
public class JwksController {

    private final JwtKeyProvider jwtKeyProvider;

    public JwksController(JwtKeyProvider jwtKeyProvider) {
        this.jwtKeyProvider = jwtKeyProvider;
    }

    @Operation(summary = "Retrieve the JSON Web Key Set (JWKS)",
            description = "Provides the public keys in JWKS format used to verify JWT tokens issued by the authentication service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "JWKS retrieved successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(example = """
                                            {
                                              "keys": [
                                                {
                                                  "kty": "RSA",
                                                  "e": "AQAB",
                                                  "use": "sig",
                                                  "kid": "rsa-key-1",
                                                  "n": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A..."
                                                }
                                              ]
                                            }
                                            """))),
                    @ApiResponse(responseCode = "500", description = "Internal server error while generating JWKS",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            })
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        RSAPublicKey publicKey = (RSAPublicKey) jwtKeyProvider.getPublicKey();

        JWK jwk = new RSAKey.Builder(publicKey)
                .keyID("rsa-key-1") // Identificador único de la clave
                .build();

        JWKSet jwkSet = new JWKSet(jwk);
        return jwkSet.toJSONObject();
    }
}
