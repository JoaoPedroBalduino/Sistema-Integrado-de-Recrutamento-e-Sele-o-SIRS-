package br.com.sirs.api.model.request;

import br.com.sirs.api.model.enums.StatusVaga;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public record RequisicaoAlteracaoStatusVaga(
    @NotNull StatusVaga status, @PastOrPresent LocalDate dataFechamento) {}
