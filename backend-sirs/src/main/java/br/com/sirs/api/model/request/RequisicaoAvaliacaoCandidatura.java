package br.com.sirs.api.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public record RequisicaoAvaliacaoCandidatura(
    @NotEmpty List<@Valid @NotNull RequisicaoNotaCriterio> notas) {}
