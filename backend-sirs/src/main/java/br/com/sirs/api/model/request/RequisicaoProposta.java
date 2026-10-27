package br.com.sirs.api.model.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record RequisicaoProposta(
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal salarioOfertado,
    @NotNull @PastOrPresent LocalDate dataProposta) {}
