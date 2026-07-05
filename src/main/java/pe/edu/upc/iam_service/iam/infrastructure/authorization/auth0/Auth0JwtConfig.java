package pe.edu.upc.iam_service.iam.infrastructure.authorization.auth0;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;

@Configuration
public class Auth0JwtConfig {

    @Bean
    public JwtDecoder auth0JwtDecoder(
            @Value("${auth0.issuer-uri}") String issuerUri,
            @Value("${auth0.audience}") String audience) {
        var jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new AudienceValidator(audience)
        ));
        return jwtDecoder;
    }

    private static final class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String audience;
        private final OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "The required audience is missing",
                null
        );

        private AudienceValidator(String audience) {
            this.audience = audience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            var audiences = token.getAudience();
            if (audiences == null) {
                audiences = List.of();
            }
            return audiences.contains(audience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(error);
        }
    }
}
