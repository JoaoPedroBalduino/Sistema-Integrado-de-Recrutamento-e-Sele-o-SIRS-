package br.com.sirs.api.service;

import static org.junit.jupiter.api.Assertions.*;

import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.enums.ChaveParametroCusto;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalculadoraCustoTest {
  private final CalculadoraCusto calculadora = new CalculadoraCusto();

  @Test
  void calculaCustosSemArredondamentoPrematuro() {
    var resultado = calculadora.calcular(Map.of(), null);
    assertEquals(new BigDecimal("513.86"), resultado.custoHoras());
    assertEquals(new BigDecimal("573.86"), resultado.total());
  }

  @Test
  void substituiDivulgacaoPeloInvestimentoRealizado() {
    assertEquals(
        new BigDecimal("963.86"), calculadora.calcular(Map.of(), new BigDecimal("400")).total());
  }

  @Test
  void rejeitaBaseMensalZero() {
    assertThrows(
        RegraNegocioException.class,
        () ->
            calculadora.calcular(Map.of(ChaveParametroCusto.HORAS_MENSAIS, BigDecimal.ZERO), null));
  }

  @Test
  void utilizaSalarioPorPapel() {
    assertTrue(
        calculadora
                .calcular(Map.of(ChaveParametroCusto.SALARIO_GESTOR, new BigDecimal("2800")), null)
                .total()
                .compareTo(new BigDecimal("573.86"))
            > 0);
  }

  @Test
  void rejeitaQuantidadeFracionada() {
    assertThrows(
        RegraNegocioException.class,
        () -> ChaveParametroCusto.CANDIDATOS_DESAFIO.validar(new BigDecimal("1.5")));
  }
}
