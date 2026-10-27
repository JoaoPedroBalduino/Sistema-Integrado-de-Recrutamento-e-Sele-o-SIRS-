package br.com.sirs.api.model.request;

import br.com.sirs.api.model.enums.Perfil;
import jakarta.validation.constraints.*;

public record RequisicaoCadastroUsuario(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 12, max = 72) String senha,
    @NotNull Perfil perfil) {}
