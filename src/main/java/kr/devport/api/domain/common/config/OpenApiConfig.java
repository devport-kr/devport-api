package kr.devport.api.domain.common.config;

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
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("DevPort API")
                        .description("Backend API for DevPort - Developer Portfolio Platform\n\n" +
                                "## Authentication\n" +
                                "This API uses OAuth2 authentication with GitHub and Google. " +
                                "Access tokens are returned in JSON responses and refresh tokens are stored in an HttpOnly cookie. " +
                                "Use the access token in the Authorization header as: `Bearer {token}`\n\n" +
                                "## OAuth2 Flow\n" +
                                "1. Initiate login: `GET /oauth2/authorization/{provider}` (github or google)\n" +
                                "2. User authenticates with provider\n" +
                                "3. Backend redirects to frontend with a one-time code: `/oauth2/redirect?code={opaque}`\n" +
                                "4. Frontend exchanges the code via `POST /api/auth/oauth2/exchange`\n" +
                                "5. Backend returns an access token and sets the refresh-token cookie\n" +
                                "6. Use access token for authenticated endpoints: `Authorization: Bearer {accessToken}`\n" +
                                "7. Refresh the access token with `POST /api/auth/refresh` (cookie-based)")
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
