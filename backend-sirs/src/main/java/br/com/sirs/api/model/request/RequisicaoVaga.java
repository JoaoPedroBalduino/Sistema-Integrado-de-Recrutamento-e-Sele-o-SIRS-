package br.com.sirs.api.model.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record RequisicaoVaga(
    @NotBlank @Size(max = 200) String titulo,
    @NotNull @Positive Long cargoId,
    @NotNull @Positive Long areaId,
    @NotNull @Positive Long solicitanteId,
    @NotNull @Positive Long recrutadorId,
    @Min(1) int posicoes,
    @Size(max = 100) String cidade,
    @Pattern(
            regexp =
                "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO")
        String estado,
    boolean remoto,
    @Size(max = 10000) String observacoes,
    @PastOrPresent LocalDate dataAbertura,
    @NotNull @Min(1) Integer prazoAlvoDias,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal investimentoDivulgacao,
    @NotNull Set<@NotNull @Positive Long> canalIds) {}
