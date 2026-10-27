package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.VisitasCanal;
import java.time.LocalDate;

public record RespostaVisitasCanal(
    Long identificador, Long canalId, LocalDate periodo, int visitas) {
  public static RespostaVisitasCanal from(VisitasCanal visitas) {
    return new RespostaVisitasCanal(
        visitas.getId(), visitas.getCanalId(), visitas.getPeriodo(), visitas.getVisitas());
  }
}
