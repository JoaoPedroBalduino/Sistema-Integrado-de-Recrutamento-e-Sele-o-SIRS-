package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoAjustesCusto;
import br.com.sirs.api.model.response.RespostaEstimativaCusto;
import br.com.sirs.api.service.CustoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vagas/{vagaId}/custos")
@RequiredArgsConstructor
public class CustoController {

  private final CustoService custos;
  private final Clock relogio;

  @GetMapping
  public RespostaEstimativaCusto consultar(
      @PathVariable Long vagaId,
      @RequestParam(required = false) LocalDate referencia,
      @RequestParam(defaultValue = "false") boolean investimentoRealizado) {
    return custos.consultar(
        vagaId, referencia == null ? LocalDate.now(relogio) : referencia, investimentoRealizado);
  }

  @PutMapping("/parametros")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void ajustar(
      @PathVariable Long vagaId, @Valid @RequestBody RequisicaoAjustesCusto requisicao) {
    custos.ajustar(vagaId, requisicao.parametros());
  }
}
