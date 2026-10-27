package br.com.sirs.api.model.request;

import br.com.sirs.api.model.enums.ChaveParametroCusto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

public record RequisicaoAjustesCusto(
    @NotEmpty Map<
                @NotNull ChaveParametroCusto,
                @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 4) BigDecimal>
            parametros) {}
