package br.com.sirs.api.model.enums;

import lombok.Getter;

@Getter
public enum TipoConfiguracaoTemporal {
  PARAMETRO("parametro_custo", "chave"),
  META("meta", "indicador");
  private final String tabela;
  private final String coluna;

  TipoConfiguracaoTemporal(String tabela, String coluna) {
    this.tabela = tabela;
    this.coluna = coluna;
  }
}
