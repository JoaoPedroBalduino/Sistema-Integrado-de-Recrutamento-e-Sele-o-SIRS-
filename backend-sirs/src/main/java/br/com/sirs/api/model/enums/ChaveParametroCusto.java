package br.com.sirs.api.model.enums;

import br.com.sirs.api.exception.RegraNegocioException;
import java.math.BigDecimal;

public enum ChaveParametroCusto {
  SALARIO_GESTOR("1400"),
  SALARIO_RECRUTADOR("1400"),
  SALARIO_COLABORADOR("1400"),
  SALARIO_DESIGNER("1400"),
  FATOR_ENCARGOS("1.7"),
  HORAS_MENSAIS("220"),
  KIT_BOAS_VINDAS("50"),
  VERBA_DIVULGACAO("10"),
  OUTROS_INVESTIMENTOS("40"),
  CURRICULOS_TRIADOS("300"),
  CANDIDATOS_DESAFIO("10"),
  CANDIDATOS_ENTREVISTA("5"),
  CANDIDATOS_FIT_CULTURAL("3"),
  COLABORADORES_FIT_CULTURAL("3"),
  HORAS_BRIEFING_GESTOR("0.5"),
  HORAS_BRIEFING_RECRUTADOR("0.5"),
  HORAS_CADASTRO("1.5"),
  HORAS_CRIATIVO("1"),
  MINUTOS_TRIAGEM("3"),
  HORAS_DINAMICA("3"),
  PARTICIPANTES_DINAMICA("2"),
  HORAS_PREPARACAO_DINAMICA("1.5"),
  HORAS_DESAFIO("0.25"),
  HORAS_ENTREVISTA("0.75"),
  HORAS_FIT_CULTURAL("1"),
  HORAS_PROPOSTA("1"),
  HORAS_ORIENTACOES("1.5");
  private final BigDecimal valorPadrao;

  ChaveParametroCusto(String valorPadrao) {
    this.valorPadrao = new BigDecimal(valorPadrao);
  }

  public BigDecimal valorPadrao() {
    return valorPadrao;
  }

  public void validar(BigDecimal valor) {
    if (valor.signum() < 0
        || ((this == HORAS_MENSAIS || this == FATOR_ENCARGOS) && valor.signum() == 0))
      throw new RegraNegocioException("Parametro deve ser positivo: " + name());
    if ((name().startsWith("CANDIDATOS_")
            || this == CURRICULOS_TRIADOS
            || name().startsWith("PARTICIPANTES_")
            || name().startsWith("COLABORADORES_"))
        && valor.stripTrailingZeros().scale() > 0)
      throw new RegraNegocioException("Quantidade deve ser inteira: " + name());
  }
}
