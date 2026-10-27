package br.com.sirs.api.model.request;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.model.enums.TipoSolicitacao;
import jakarta.validation.constraints.*;
import java.time.*;

public record RequisicaoSolicitacaoTitular(
    @NotNull @Positive Long candidatoId, @NotNull TipoSolicitacao tipo) {}
