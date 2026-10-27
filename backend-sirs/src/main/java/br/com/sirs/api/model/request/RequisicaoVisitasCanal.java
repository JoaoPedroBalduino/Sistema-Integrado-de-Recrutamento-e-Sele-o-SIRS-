package br.com.sirs.api.model.request;

import static br.com.sirs.api.model.enums.Perfil.*;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RequisicaoVisitasCanal(
    @NotNull @Positive Long canalId,
    @NotNull @PastOrPresent LocalDate periodo,
    @Min(0) int visitas) {}
