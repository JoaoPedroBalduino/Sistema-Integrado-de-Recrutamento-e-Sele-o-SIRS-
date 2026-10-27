package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.AvaliacaoCriterio;
import java.time.*;

public record RespostaAvaliacaoCriterio(Long criterioId, short nota, Long avaliadorId) {
  public static RespostaAvaliacaoCriterio from(AvaliacaoCriterio avaliacao) {
    return new RespostaAvaliacaoCriterio(
        avaliacao.getCriterioId(), avaliacao.getNota(), avaliacao.getAvaliadorId());
  }
}
