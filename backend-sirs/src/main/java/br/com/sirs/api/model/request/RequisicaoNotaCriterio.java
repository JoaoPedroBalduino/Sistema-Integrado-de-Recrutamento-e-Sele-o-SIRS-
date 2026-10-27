package br.com.sirs.api.model.request;

import jakarta.validation.constraints.*;
import java.time.*;

public record RequisicaoNotaCriterio(
    @NotNull @Positive Long criterioId, @Min(0) @Max(5) short nota) {}
