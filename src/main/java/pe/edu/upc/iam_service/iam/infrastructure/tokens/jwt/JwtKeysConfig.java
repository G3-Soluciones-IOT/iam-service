package pe.edu.upc.iam_service.iam.infrastructure.tokens.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import pe.edu.upc.iam_service.iam.infrastructure.tokens.jwt.services.JwtKeyProvider;

@Configuration
public class JwtKeysConfig {

    @Bean
    public JwtKeyProvider jwtKeyProvider(
            ResourceLoader resourceLoader,
            @Value("${authorization.jwt.private-key-path}") String privateKeyPath,
            @Value("${authorization.jwt.public-key-path}") String publicKeyPath) {

        return new JwtKeyProvider(resourceLoader, privateKeyPath, publicKeyPath);
    }
}
