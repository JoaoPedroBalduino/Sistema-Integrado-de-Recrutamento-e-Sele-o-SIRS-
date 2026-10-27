package br.com.sirs.api.model.request;

import jakarta.validation.constraints.*;

public record RequisicaoAlteracaoSenha(
    @NotBlank @Size(max = 72) String senhaAtual,
    @NotBlank @Size(min = 12, max = 72) String novaSenha) {}
