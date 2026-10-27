package br.com.sirs.api.model.request;

import static br.com.sirs.api.model.enums.Perfil.*;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.*;

public record RequisicaoConfiguracaoTemporal(
    @NotBlank String chave,
    @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 4) BigDecimal valor,
    @NotNull LocalDate vigenciaInicio,
    LocalDate vigenciaFim) {}
