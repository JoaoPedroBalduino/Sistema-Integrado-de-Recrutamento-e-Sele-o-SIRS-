package br.com.sirs.api.service;

import static org.junit.jupiter.api.Assertions.*;

import br.com.sirs.api.model.value.NotaPonderada;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraNotaTest {
  private final CalculadoraNota calculadora = new CalculadoraNota();

  @Test
  void calculaExemploDoPrototipo() {
    assertEquals(
        new BigDecimal("0.6842"),
        calculadora.calcular(
            List.of(
                new NotaPonderada(4, 4),
                new NotaPonderada(2, 5),
                new NotaPonderada(4, 3),
                new NotaPonderada(3, 4),
                new NotaPonderada(5, 3))));
  }

  @Test
  void mantemNotaAusenteQuandoTodasSaoZero() {
    assertNull(calculadora.calcular(List.of(new NotaPonderada(0, 4), new NotaPonderada(0, 5))));
  }

  @Test
  void consideraCriterioSemNotaComoZero() {
    assertEquals(
        new BigDecimal("0.5000"),
        calculadora.calcular(List.of(new NotaPonderada(5, 2), new NotaPonderada(0, 2))));
  }

  @Test
  void rejeitaEscalasInvalidas() {
    assertThrows(
        IllegalArgumentException.class,
        () -> calculadora.calcular(List.of(new NotaPonderada(6, 1))));
    assertThrows(
        IllegalArgumentException.class,
        () -> calculadora.calcular(List.of(new NotaPonderada(1, 0))));
  }

  @Test
  void retornaAusenciaParaListaVazia() {
    assertNull(calculadora.calcular(List.of()));
  }
}
