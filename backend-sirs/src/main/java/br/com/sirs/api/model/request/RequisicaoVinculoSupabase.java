package br.com.sirs.api.model.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequisicaoVinculoSupabase(@NotNull UUID usuarioSupabaseId) {}
