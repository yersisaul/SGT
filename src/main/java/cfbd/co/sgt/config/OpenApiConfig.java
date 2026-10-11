package cfbd.co.sgt.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(
                new Components()
                    .addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            );
    }

    // Selector de Swagger UI: SGT (tickets), panel del sitio web y API pública del sitio.

    @Bean
    public GroupedOpenApi grupoSgt() {
        return GroupedOpenApi.builder()
                .group("1-sgt")
                .displayName("SGT · Sistema de tickets")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/web/**", "/api/public/**", "/api/auth/registro-cliente")
                .build();
    }

    @Bean
    public GroupedOpenApi grupoWebAdmin() {
        return GroupedOpenApi.builder()
                .group("2-web-admin")
                .displayName("Sitio web · Panel de administración (JWT + permisos web_*)")
                .pathsToMatch("/api/web/**")
                .build();
    }

    @Bean
    public GroupedOpenApi grupoWebPublica() {
        return GroupedOpenApi.builder()
                .group("3-web-publica")
                .displayName("Sitio web · API pública (sin login)")
                .pathsToMatch("/api/public/**", "/api/auth/registro-cliente")
                // Sin token: se quita el candado de JWT que el OpenAPI global pone a todo.
                .addOpenApiCustomizer(api -> api.getPaths().values()
                        .forEach(path -> path.readOperations().forEach(op -> op.setSecurity(List.of()))))
                .build();
    }
}
