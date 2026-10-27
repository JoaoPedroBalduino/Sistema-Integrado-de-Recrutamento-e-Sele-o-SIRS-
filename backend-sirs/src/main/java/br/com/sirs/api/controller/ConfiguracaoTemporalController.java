package br.com.sirs.api.controller;

import br.com.sirs.api.model.enums.TipoConfiguracaoTemporal;
import br.com.sirs.api.model.request.RequisicaoConfiguracaoTemporal;
import br.com.sirs.api.model.request.RequisicaoEncerramentoVigencia;
import br.com.sirs.api.model.response.RespostaConfiguracaoTemporal;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.service.ConfiguracaoTemporalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Parâmetros e metas",
    description =
        "Configurações com vigência inclusiva por data. ADMIN e COORD_RH alteram os registros; períodos sobrepostos são rejeitados.")
@RestController
@RequestMapping("/api/v1/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoTemporalController {
  private final ConfiguracaoTemporalService configuracoes;

  @Operation(summary = "Listar parâmetros de custo", operationId = "listarParametrosDeCusto")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/parametros-custo")
  public RespostaPaginada<RespostaConfiguracaoTemporal> listarParametros(
      @ParameterObject Pageable paginacao) {
    return configuracoes.listar(TipoConfiguracaoTemporal.PARAMETRO, paginacao);
  }

  @Operation(summary = "Listar metas dos indicadores", operationId = "listarMetasDosIndicadores")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/metas")
  public RespostaPaginada<RespostaConfiguracaoTemporal> listarMetas(
      @ParameterObject Pageable paginacao) {
    return configuracoes.listar(TipoConfiguracaoTemporal.META, paginacao);
  }

  @Operation(summary = "Cadastrar parâmetro de custo", operationId = "cadastrarParametroDeCusto")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/parametros-custo")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaConfiguracaoTemporal cadastrarParametro(
      @Valid @RequestBody RequisicaoConfiguracaoTemporal requisicao) {
    return configuracoes.cadastrar(TipoConfiguracaoTemporal.PARAMETRO, requisicao);
  }

  @Operation(summary = "Cadastrar meta de indicador", operationId = "cadastrarMetaDeIndicador")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/metas")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaConfiguracaoTemporal cadastrarMeta(
      @Valid @RequestBody RequisicaoConfiguracaoTemporal requisicao) {
    return configuracoes.cadastrar(TipoConfiguracaoTemporal.META, requisicao);
  }

  @Operation(
      summary = "Encerrar vigência do parâmetro",
      operationId = "encerrarVigenciaDoParametro")
  @ApiResponse(
      responseCode = "204",
      description = "Operação concluída, sem corpo de resposta.",
      content = @Content)
  @PatchMapping("/parametros-custo/{identificador}/vigencia")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void encerrarParametro(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoEncerramentoVigencia requisicao) {
    configuracoes.encerrar(TipoConfiguracaoTemporal.PARAMETRO, identificador, requisicao);
  }

  @Operation(summary = "Encerrar vigência da meta", operationId = "encerrarVigenciaDaMeta")
  @ApiResponse(
      responseCode = "204",
      description = "Operação concluída, sem corpo de resposta.",
      content = @Content)
  @PatchMapping("/metas/{identificador}/vigencia")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void encerrarMeta(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoEncerramentoVigencia requisicao) {
    configuracoes.encerrar(TipoConfiguracaoTemporal.META, identificador, requisicao);
  }
}
