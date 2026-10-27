package br.com.sirs.api.config;

import jakarta.validation.constraints.*;
import java.net.URI;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "sirs.supabase")
@Validated
@Getter
@Setter
public class PropriedadesSupabase {
  @NotNull private URI url;
  @NotNull private URI jwksUrl;
  @NotBlank private String publishableKey;
  @NotBlank private String secretKey;

  public String emissor() {
    return url.toString().replaceAll("/$", "") + "/auth/v1";
  }
}
