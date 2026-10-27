package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.SolicitacaoTitular;
import br.com.sirs.api.model.enums.TipoSolicitacao;
import java.time.*;

public record RespostaSolicitacaoTitular(
    Long identificador,
    Long candidatoId,
    TipoSolicitacao tipo,
    String status,
    Instant solicitadaEm,
    LocalDate prazo,
    Instant concluidaEm) {
  public static RespostaSolicitacaoTitular from(SolicitacaoTitular solicitacao) {
    return new RespostaSolicitacaoTitular(
        solicitacao.getId(),
        solicitacao.getCandidatoId(),
        solicitacao.getTipo(),
        solicitacao.getStatus(),
        solicitacao.getSolicitadaEm(),
        solicitacao.getPrazo(),
        solicitacao.getConcluidaEm());
  }
}
