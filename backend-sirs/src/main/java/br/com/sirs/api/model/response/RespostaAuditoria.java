package br.com.sirs.api.model.response;

import java.time.Instant;

public record RespostaAuditoria(
    Long identificador,
    Long usuarioId,
    String entidade,
    Long entidadeId,
    String acao,
    Instant ocorridaEm) {}
