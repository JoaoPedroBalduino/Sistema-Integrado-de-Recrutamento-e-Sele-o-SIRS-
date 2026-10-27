package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.HistoricoStatusVaga;
import java.time.*;
import java.util.*;

public record RespostaHistoricoVaga(
    Long identificador,
    String statusAnterior,
    String statusNovo,
    Long usuarioId,
    Instant alteradoEm) {
  public static RespostaHistoricoVaga of(HistoricoStatusVaga historico) {
    return new RespostaHistoricoVaga(
        historico.getId(),
        historico.getStatusAnterior(),
        historico.getStatusNovo(),
        historico.getUsuarioId(),
        historico.getAlteradoEm());
  }
}
