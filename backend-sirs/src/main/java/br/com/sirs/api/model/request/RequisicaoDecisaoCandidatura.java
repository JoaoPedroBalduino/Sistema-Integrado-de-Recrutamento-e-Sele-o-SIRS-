package br.com.sirs.api.model.request;

import br.com.sirs.api.model.enums.StatusCandidatura;
import jakarta.validation.constraints.*;
import java.time.*;

public record RequisicaoDecisaoCandidatura(@NotNull StatusCandidatura status) {}
