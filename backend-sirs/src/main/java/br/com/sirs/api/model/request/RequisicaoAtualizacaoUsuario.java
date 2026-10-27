package br.com.sirs.api.model.request;

import br.com.sirs.api.model.enums.Perfil;
import jakarta.validation.constraints.*;

public record RequisicaoAtualizacaoUsuario(
    @NotBlank @Size(max = 150) String nome,
    @NotNull Perfil perfil,
    @NotNull Boolean ativo,
    @NotNull Boolean bloqueado) {}
