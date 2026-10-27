package br.com.sirs.api.service;

import br.com.sirs.api.model.value.NotaPonderada;
import java.math.*;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraNota {

  /** RN-04: criterios sem nota contam como zero; soma zero permanece sem nota final. */
  public BigDecimal calcular(List<NotaPonderada> notas) {
    long somaPonderada = 0;
    long somaPesos = 0;
    for (var avaliacao : notas) {
      if (avaliacao.nota() < 0
          || avaliacao.nota() > 5
          || avaliacao.peso() < 1
          || avaliacao.peso() > 5) throw new IllegalArgumentException("Nota ou peso invalido.");
      somaPonderada += (long) avaliacao.nota() * avaliacao.peso();
      somaPesos += avaliacao.peso();
    }
    if (somaPonderada == 0 || somaPesos == 0) return null;
    return BigDecimal.valueOf(somaPonderada)
        .divide(BigDecimal.valueOf(5 * somaPesos), 4, RoundingMode.HALF_UP);
  }
}
