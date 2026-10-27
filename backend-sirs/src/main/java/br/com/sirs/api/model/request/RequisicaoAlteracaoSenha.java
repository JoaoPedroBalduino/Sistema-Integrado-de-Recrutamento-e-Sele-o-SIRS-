package br.com.sirs.api.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record RequisicaoAlteracaoSenha(
    @NotBlank @Size(max = 72) @Schema(
            format = "password",
            accessMode = Schema.AccessMode.WRITE_ONLY,
            description = "Senha enviada apenas na requisição; nunca retornada pela API.")
        String senhaAtual,
    @NotBlank @Size(min = 12, max = 72) @Schema(
            format = "password",
            accessMode = Schema.AccessMode.WRITE_ONLY,
            description = "Senha enviada apenas na requisição; nunca retornada pela API.")
        String novaSenha) {}
