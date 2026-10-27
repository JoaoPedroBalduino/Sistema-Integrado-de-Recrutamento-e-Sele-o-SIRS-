package br.com.sirs.api.config;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.*;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.*;

@OpenAPIDefinition(
    info =
        @Info(
            title = "SIRS API",
            version = "v1",
            description = "Recrutamento, selecao e indicadores"),
    security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@Configuration
public class ConfiguracaoDocumentacao {
  @Bean
  Clock relogio() {
    return Clock.system(ZoneId.of("America/Sao_Paulo"));
  }
}
