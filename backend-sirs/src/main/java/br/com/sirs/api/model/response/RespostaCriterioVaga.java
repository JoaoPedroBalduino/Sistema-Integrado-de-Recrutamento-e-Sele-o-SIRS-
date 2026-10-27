package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.CriterioVaga;
import java.time.*;
import java.util.*;

public record RespostaCriterioVaga(Long identificador, String nome, String descricao, short peso) {
  public static RespostaCriterioVaga of(CriterioVaga criterio) {
    return new RespostaCriterioVaga(
        criterio.getId(), criterio.getNome(), criterio.getDescricao(), criterio.getPeso());
  }
}
