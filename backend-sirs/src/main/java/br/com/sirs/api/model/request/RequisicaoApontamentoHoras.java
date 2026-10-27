package br.com.sirs.api.model.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record RequisicaoApontamentoHoras(
    @NotNull @PastOrPresent LocalDate data,
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("24") @Digits(integer = 2, fraction = 2) BigDecimal horas,
    @Size(max = 200) String atividade) {}
