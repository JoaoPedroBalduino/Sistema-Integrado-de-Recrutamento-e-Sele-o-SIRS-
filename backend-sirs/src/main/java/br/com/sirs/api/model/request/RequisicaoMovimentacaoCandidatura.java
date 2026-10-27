package br.com.sirs.api.model.request;

import jakarta.validation.constraints.*;
import java.time.*;

public record RequisicaoMovimentacaoCandidatura(@NotNull @Positive Long etapaId) {}
