package br.com.sirs.api.model.request;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public record RequisicaoCadastroEtapa(
    @NotBlank @Size(max = 150) String nome,
    @Size(max = 5000) String descricao,
    @Min(1) int ordem) {}
