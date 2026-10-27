package br.com.sirs.api.model.request;

import static br.com.sirs.api.model.enums.Perfil.*;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.data.domain.*;

public record RequisicaoEncerramentoVigencia(@NotNull LocalDate vigenciaFim) {}
