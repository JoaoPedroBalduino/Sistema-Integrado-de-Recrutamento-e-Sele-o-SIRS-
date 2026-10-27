package br.com.sirs.api.controller;

import br.com.sirs.api.model.response.RespostaIndicadorCanal;
import br.com.sirs.api.model.response.RespostaIndicadorCargo;
import br.com.sirs.api.model.response.RespostaIndicadorRecrutador;
import br.com.sirs.api.model.response.RespostaIndicadorVaga;
import br.com.sirs.api.model.response.RespostaResumoIndicadores;
import br.com.sirs.api.service.IndicadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Indicadores",
    description =
        "Relatórios por período, com metas vigentes. Os intervalos aceitam até 36 meses. Gestores ficam limitados às próprias vagas.")
@RestController
@RequestMapping("/api/v1/indicadores")
@RequiredArgsConstructor
public class IndicadorController {
  private final IndicadorService indicadores;

  @Operation(
      summary = "Consultar resumo dos indicadores",
      operationId = "consultarResumoDosIndicadores")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/resumo")
  public RespostaResumoIndicadores resumir(
      @RequestParam LocalDate de, @RequestParam LocalDate ate) {
    return indicadores.resumir(de, ate);
  }

  @Operation(
      summary = "Consultar histórico mensal de indicadores",
      operationId = "consultarHistoricoMensalDeIndicadores")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/historico")
  public List<RespostaResumoIndicadores> historico(
      @RequestParam LocalDate de, @RequestParam LocalDate ate) {
    return indicadores.historico(de, ate);
  }

  @Operation(
      summary = "Consultar indicadores por cargo",
      operationId = "consultarIndicadoresPorCargo")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/cargos")
  public List<RespostaIndicadorCargo> cargos(
      @RequestParam LocalDate de, @RequestParam LocalDate ate) {
    return indicadores.cargos(de, ate);
  }

  @Operation(
      summary = "Consultar indicadores por recrutador",
      operationId = "consultarIndicadoresPorRecrutador")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/recrutadores")
  public List<RespostaIndicadorRecrutador> recrutadores(
      @RequestParam LocalDate de, @RequestParam LocalDate ate) {
    return indicadores.recrutadores(de, ate);
  }

  @Operation(
      summary = "Consultar conversão por canal",
      operationId = "consultarConversaoPorCanal",
      description =
          "Visitas são mensais; contratações usam as datas exatas do intervalo. Não disponível para GESTOR.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/canais")
  public List<RespostaIndicadorCanal> canais(
      @RequestParam LocalDate de, @RequestParam LocalDate ate) {
    return indicadores.canais(de, ate);
  }

  @Operation(
      summary = "Consultar funil e avaliações da vaga",
      operationId = "consultarFunilEAvaliacoesDaVaga")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/vagas/{vagaId}")
  public RespostaIndicadorVaga vaga(@PathVariable Long vagaId) {
    return indicadores.vaga(vagaId);
  }
}
