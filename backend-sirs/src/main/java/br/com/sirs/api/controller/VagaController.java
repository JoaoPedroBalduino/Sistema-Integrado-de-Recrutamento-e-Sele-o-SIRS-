package br.com.sirs.api.controller;

import br.com.sirs.api.model.enums.StatusVaga;
import br.com.sirs.api.model.request.RequisicaoAlteracaoStatusVaga;
import br.com.sirs.api.model.request.RequisicaoApontamentoHoras;
import br.com.sirs.api.model.request.RequisicaoCadastroCriterio;
import br.com.sirs.api.model.request.RequisicaoCadastroEtapa;
import br.com.sirs.api.model.request.RequisicaoVaga;
import br.com.sirs.api.model.response.RespostaApontamentoHoras;
import br.com.sirs.api.model.response.RespostaCriterioVaga;
import br.com.sirs.api.model.response.RespostaDetalheVaga;
import br.com.sirs.api.model.response.RespostaEtapaVaga;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.model.response.RespostaVaga;
import br.com.sirs.api.service.VagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Vagas",
    description = "Ciclo de vida da vaga, estrutura de avaliação e apontamentos de horas.")
@RestController
@RequestMapping("/api/v1/vagas")
@RequiredArgsConstructor
public class VagaController {
  private final VagaService vagas;

  @Operation(
      summary = "Listar vagas",
      operationId = "listarVagas",
      description =
          "Filtra por status, área, cargo, recrutador e intervalo de abertura, com paginação e ordenação.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping
  public RespostaPaginada<RespostaVaga> listar(
      @RequestParam(required = false) StatusVaga status,
      @RequestParam(required = false) Long areaId,
      @RequestParam(required = false) Long cargoId,
      @RequestParam(required = false) Long recrutadorId,
      @RequestParam(required = false) LocalDate de,
      @RequestParam(required = false) LocalDate ate,
      @PageableDefault(sort = "id") @ParameterObject Pageable paginacao) {
    return vagas.listar(status, areaId, cargoId, recrutadorId, de, ate, paginacao);
  }

  @Operation(
      summary = "Consultar detalhes da vaga",
      operationId = "consultarDetalhesDaVaga",
      description =
          "Inclui etapas, critérios, histórico de status, horas e contagens calculadas de candidatos e contratações.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/{identificador}")
  public RespostaDetalheVaga detalhe(@PathVariable Long identificador) {
    return vagas.detalhe(identificador);
  }

  @Operation(
      summary = "Cadastrar vaga",
      operationId = "cadastrarVaga",
      description =
          "Cria em RASCUNHO. Exige COORD_RH, RECRUTADOR responsável ou GESTOR solicitante.")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping
  public ResponseEntity<RespostaVaga> create(@Valid @RequestBody RequisicaoVaga requisicao) {
    var vaga = vagas.cadastrar(requisicao);
    return ResponseEntity.created(URI.create("/api/v1/vagas/" + vaga.identificador())).body(vaga);
  }

  @Operation(
      summary = "Atualizar dados da vaga",
      operationId = "atualizarDadosDaVaga",
      description = "Dados estruturais são editáveis somente em rascunho.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PutMapping("/{identificador}")
  public RespostaVaga update(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoVaga requisicao) {
    return vagas.atualizar(identificador, requisicao);
  }

  @Operation(
      summary = "Alterar status da vaga",
      operationId = "alterarStatusDaVaga",
      description =
          "Aprovar ou devolver uma vaga exige COORD_RH ou DIRETORIA. Publicação exige canais, etapas e critérios. FECHADA é um estado final.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PatchMapping("/{identificador}/status")
  public RespostaVaga status(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoAlteracaoStatusVaga requisicao) {
    return vagas.alterarStatus(identificador, requisicao);
  }

  @Operation(
      summary = "Cadastrar etapa da vaga",
      operationId = "cadastrarEtapaDaVaga",
      description = "Somente em rascunho. A ordem deve ser positiva e única por vaga.")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/{identificador}/etapas")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaEtapaVaga etapa(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoCadastroEtapa requisicao) {
    return vagas.cadastrarEtapa(identificador, requisicao);
  }

  @Operation(
      summary = "Cadastrar critério da vaga",
      operationId = "cadastrarCriterioDaVaga",
      description = "Somente em rascunho. O peso deve estar entre 1 e 5.")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/{identificador}/criterios")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaCriterioVaga criterio(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoCadastroCriterio requisicao) {
    return vagas.cadastrarCriterio(identificador, requisicao);
  }

  @Operation(
      summary = "Excluir etapa da vaga",
      operationId = "excluirEtapaDaVaga",
      description = "Somente em rascunho.")
  @ApiResponse(
      responseCode = "204",
      description = "Operação concluída, sem corpo de resposta.",
      content = @Content)
  @DeleteMapping("/{identificador}/etapas/{etapaId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluirEtapa(@PathVariable Long identificador, @PathVariable Long etapaId) {
    vagas.excluirEtapa(identificador, etapaId);
  }

  @Operation(
      summary = "Excluir critério da vaga",
      operationId = "excluirCriterioDaVaga",
      description = "Somente em rascunho.")
  @ApiResponse(
      responseCode = "204",
      description = "Operação concluída, sem corpo de resposta.",
      content = @Content)
  @DeleteMapping("/{identificador}/criterios/{criterioId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluirCriterio(@PathVariable Long identificador, @PathVariable Long criterioId) {
    vagas.excluirCriterio(identificador, criterioId);
  }

  @Operation(
      summary = "Registrar horas de trabalho na vaga",
      operationId = "registrarHorasDeTrabalhoNaVaga",
      description =
          "Exige COORD_RH ou RECRUTADOR responsável. Horas maiores que zero e de até 24 por apontamento.")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/{identificador}/horas")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaApontamentoHoras horas(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoApontamentoHoras requisicao) {
    return vagas.registrarHoras(identificador, requisicao);
  }

  @Operation(
      summary = "Atualizar etapa da vaga",
      operationId = "atualizarEtapaDaVaga",
      description = "Somente em rascunho.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PutMapping("/{identificador}/etapas/{etapaId}")
  public RespostaEtapaVaga atualizarEtapa(
      @PathVariable Long identificador,
      @PathVariable Long etapaId,
      @Valid @RequestBody RequisicaoCadastroEtapa requisicao) {
    return vagas.atualizarEtapa(identificador, etapaId, requisicao);
  }

  @Operation(
      summary = "Atualizar critério da vaga",
      operationId = "atualizarCriterioDaVaga",
      description = "Somente em rascunho.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PutMapping("/{identificador}/criterios/{criterioId}")
  public RespostaCriterioVaga atualizarCriterio(
      @PathVariable Long identificador,
      @PathVariable Long criterioId,
      @Valid @RequestBody RequisicaoCadastroCriterio requisicao) {
    return vagas.atualizarCriterio(identificador, criterioId, requisicao);
  }
}
