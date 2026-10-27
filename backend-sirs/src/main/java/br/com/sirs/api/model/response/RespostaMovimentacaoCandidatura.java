package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.HistoricoEtapa;
import java.time.*;

public record RespostaMovimentacaoCandidatura(Long etapaId, Long usuarioId, Instant movidoEm) {
  public static RespostaMovimentacaoCandidatura from(HistoricoEtapa historico) {
    return new RespostaMovimentacaoCandidatura(
        historico.getEtapaId(), historico.getUsuarioId(), historico.getMovidoEm());
  }
}
