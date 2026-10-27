package br.com.sirs.api.controller;

import br.com.sirs.api.model.response.RespostaIndicadorCanal;
import br.com.sirs.api.model.response.RespostaIndicadorCargo;
import br.com.sirs.api.model.response.RespostaIndicadorRecrutador;
import br.com.sirs.api.model.response.RespostaIndicadorVaga;
import br.com.sirs.api.model.response.RespostaResumoIndicadores;
import br.com.sirs.api.service.IndicadorService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/indicadores")
@RequiredArgsConstructor
public class IndicadorController {
  private final IndicadorService indicadores;

  @GetMapping("/resumo")
  public RespostaResumoIndicadores resumir(
      @RequestParam LocalDate de, @RequestParam LocalDate ate) {
    return indicadores.resumir(de, ate);
  }

  @GetMapping("/historico")
  public List<RespostaResumoIndicadores> historico(
      @RequestParam LocalDate de, @RequestParam LocalDate ate) {
    return indicadores.historico(de, ate);
  }

  @GetMapping("/cargos")
  public List<RespostaIndicadorCargo> cargos(
      @RequestParam LocalDate de, @RequestParam LocalDate ate) {
    return indicadores.cargos(de, ate);
  }

  @GetMapping("/recrutadores")
  public List<RespostaIndicadorRecrutador> recrutadores(
      @RequestParam LocalDate de, @RequestParam LocalDate ate) {
    return indicadores.recrutadores(de, ate);
  }

  @GetMapping("/canais")
  public List<RespostaIndicadorCanal> canais(
      @RequestParam LocalDate de, @RequestParam LocalDate ate) {
    return indicadores.canais(de, ate);
  }

  @GetMapping("/vagas/{vagaId}")
  public RespostaIndicadorVaga vaga(@PathVariable Long vagaId) {
    return indicadores.vaga(vagaId);
  }
}
