package br.com.sirs.api.model.request;

import static br.com.sirs.api.model.enums.Perfil.*;

import jakarta.validation.constraints.*;

public record RequisicaoCadastroApoio(
    @NotBlank @Size(max = 100) String nome, @NotNull Boolean ativo) {}
