package br.com.sirs.api.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record RequisicaoRenovacaoSessao(
    @NotBlank @Size(max = 8192) @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        String refreshToken) {}
