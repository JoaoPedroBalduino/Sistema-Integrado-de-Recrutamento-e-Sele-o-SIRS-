package br.com.sirs.api.model.request;

import br.com.sirs.api.model.enums.BaseLegal;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record RequisicaoCandidato(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 500) @Pattern(regexp = "https://[^\\s]+", message = "deve ser uma URL HTTPS") String curriculoUrl,
    @NotNull BaseLegal baseLegal,
    @NotNull @PastOrPresent Instant dataColeta,
    @Size(max = 20) String versaoConsentimento) {}
