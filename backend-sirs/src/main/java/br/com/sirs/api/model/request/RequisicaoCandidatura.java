package br.com.sirs.api.model.request;

import jakarta.validation.constraints.*;
import java.time.*;

public record RequisicaoCandidatura(
    @NotNull @Positive Long vagaId,
    @NotNull @Positive Long candidatoId,
    @NotNull @Positive Long canalId) {}
