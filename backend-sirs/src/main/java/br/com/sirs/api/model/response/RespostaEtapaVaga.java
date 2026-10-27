package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.EtapaVaga;
import java.time.*;
import java.util.*;

public record RespostaEtapaVaga(Long identificador, String nome, String descricao, int ordem) {
  public static RespostaEtapaVaga of(EtapaVaga etapa) {
    return new RespostaEtapaVaga(
        etapa.getId(), etapa.getNome(), etapa.getDescricao(), etapa.getOrdem());
  }
}
