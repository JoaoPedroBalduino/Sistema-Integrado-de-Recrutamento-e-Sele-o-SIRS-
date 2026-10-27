package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoAjustesCusto;
import br.com.sirs.api.model.response.RespostaEstimativaCusto;
import br.com.sirs.api.service.CustoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Custos",
    description =
        "Estimativas calculadas com salários por papel, encargos, horas mensais e volumes por etapa.")
@RestController
@RequestMapping("/api/v1/vagas/{vagaId}/custos")
@RequiredArgsConstructor
public class CustoController {

  private final CustoService custos;
  private final Clock relogio;

  @Operation(
      summary = "Consultar estimativa de custo da vaga",
      operationId = "consultarEstimativaDeCustoDaVaga",
      description =
          "referencia seleciona a vigência. investimentoRealizado=true substitui a verba padrão de divulgação pelo investimento efetivo da vaga.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping
  public RespostaEstimativaCusto consultar(
      @PathVariable Long vagaId,
      @RequestParam(required = false) LocalDate referencia,
      @RequestParam(defaultValue = "false") boolean investimentoRealizado) {
    return custos.consultar(
        vagaId, referencia == null ? LocalDate.now(relogio) : referencia, investimentoRealizado);
  }

  @Operation(
      summary = "Ajustar parâmetros de custo da vaga",
      operationId = "ajustarParametrosDeCustoDaVaga",
      description = "Somente COORD_RH. Não permite alterar custos de vagas fechadas.")
  @ApiResponse(
      responseCode = "204",
      description = "Operação concluída, sem corpo de resposta.",
      content = @Content)
  @PutMapping("/parametros")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void ajustar(
      @PathVariable Long vagaId, @Valid @RequestBody RequisicaoAjustesCusto requisicao) {
    custos.ajustar(vagaId, requisicao.parametros());
  }
}
