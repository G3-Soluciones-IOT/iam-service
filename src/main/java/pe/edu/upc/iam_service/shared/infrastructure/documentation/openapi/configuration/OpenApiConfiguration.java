package pe.edu.upc.iam_service.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI iamOpenApi(
            @Value("${documentation.openapi.server-url:}") String serverUrl,
            @Value("${documentation.openapi.server-description:Public Gateway}") String serverDescription) {
        final String securitySchemeName = "bearerAuth";
        var openApi = new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocs())
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));

        if (StringUtils.hasText(serverUrl)) {
            openApi.addServersItem(new Server()
                    .url(serverUrl)
                    .description(serverDescription));
        }

        return openApi;
    }

    private Info apiInfo() {
        return new Info()
                .title("IAM Service API")
                .description("REST API documentation for IAM Service")
                .version("v1.0.0")
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"))
                .contact(new Contact()
                        .name("JameoFit Team")
                        .url("https://github.com/G2-Aplicaciones-Moviles"));
    }

    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
                .description("JameoFit External Documentation")
                .url("https://github.com/G2-Aplicaciones-Moviles");
    }
}
