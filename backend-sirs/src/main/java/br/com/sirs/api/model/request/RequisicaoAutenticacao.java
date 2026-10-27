package br.com.sirs.api.model.request;

import jakarta.validation.constraints.*;

public record RequisicaoAutenticacao(
    @NotBlank @Email @Size(max = 255) String email, @NotBlank @Size(max = 72) String senha) {}
