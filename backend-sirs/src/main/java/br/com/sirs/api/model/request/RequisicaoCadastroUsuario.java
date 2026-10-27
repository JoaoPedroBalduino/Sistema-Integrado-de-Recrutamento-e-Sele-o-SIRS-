package br.com.sirs.api.model.request;

import br.com.sirs.api.model.enums.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record RequisicaoCadastroUsuario(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 12, max = 72) @Schema(
            format = "password",
            accessMode = Schema.AccessMode.WRITE_ONLY,
            description = "Senha enviada apenas na requisição; nunca retornada pela API.")
        String senha,
    @NotNull Perfil perfil) {}
