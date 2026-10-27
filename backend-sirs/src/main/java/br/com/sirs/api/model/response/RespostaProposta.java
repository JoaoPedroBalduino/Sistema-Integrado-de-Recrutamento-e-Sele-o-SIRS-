package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.Proposta;
import java.math.BigDecimal;
import java.time.*;

public record RespostaProposta(
    Long identificador, BigDecimal salarioOfertado, LocalDate dataProposta, LocalDate dataAceite) {
  public static RespostaProposta from(Proposta proposta) {
    return proposta == null
        ? null
        : new RespostaProposta(
            proposta.getId(),
            proposta.getSalarioOfertado(),
            proposta.getDataProposta(),
            proposta.getDataAceite());
  }
}
