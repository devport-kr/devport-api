package kr.devport.api.config;

import io.swagger.v3.oas.models.Components;
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

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI devPortOpenAPI() {
        // Security scheme for JWT Bearer token
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("DevPort API")
                        .description("Backend API for DevPort - Developer Portfolio Platform\n\n" +
                                "## Authentication\n" +
                                "This API uses OAuth2 authentication with GitHub and Google. " +
                                "After successful authentication, you'll receive a JWT token. " +
                                "Use this token in the Authorization header as: `Bearer {token}`\n\n" +
                                "## OAuth2 Flow\n" +
                                "1. Initiate login: `GET /oauth2/authorize/{provider}` (github or google)\n" +
                                "2. User authenticates with provider\n" +
                                "3. Callback redirects to frontend with JWT token\n" +
                                "4. Use token for authenticated endpoints")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("DevPort Team")
                                .url("https://devport.kr"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.devport.kr")
                                .description("Production Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from OAuth2 login")));
    }
}
