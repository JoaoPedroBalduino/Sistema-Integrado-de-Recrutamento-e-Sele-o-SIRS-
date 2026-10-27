package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.ApontamentoHoras;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record RespostaApontamentoHoras(
    Long identificador, Long usuarioId, LocalDate data, BigDecimal horas, String atividade) {
  public static RespostaApontamentoHoras of(ApontamentoHoras apontamento) {
    return new RespostaApontamentoHoras(
        apontamento.getId(),
        apontamento.getUsuarioId(),
        apontamento.getData(),
        apontamento.getHoras(),
        apontamento.getAtividade());
  }
}
