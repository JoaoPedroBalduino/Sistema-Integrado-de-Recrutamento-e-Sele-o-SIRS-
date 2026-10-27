package br.com.sirs.api.model.request;

import jakarta.validation.constraints.*;
import java.time.*;

public record RequisicaoContratacao(@NotNull @PastOrPresent LocalDate dataAceite) {}
