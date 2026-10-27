package br.com.sirs.api.controller;

import br.com.sirs.api.model.enums.StatusCandidatura;
import br.com.sirs.api.model.request.RequisicaoAvaliacaoCandidatura;
import br.com.sirs.api.model.request.RequisicaoCandidatura;
import br.com.sirs.api.model.request.RequisicaoContratacao;
import br.com.sirs.api.model.request.RequisicaoDecisaoCandidatura;
import br.com.sirs.api.model.request.RequisicaoDesignacaoAvaliador;
import br.com.sirs.api.model.request.RequisicaoMovimentacaoCandidatura;
import br.com.sirs.api.model.request.RequisicaoProposta;
import br.com.sirs.api.model.response.RespostaCandidatura;
import br.com.sirs.api.model.response.RespostaDetalheCandidatura;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.model.response.RespostaProposta;
import br.com.sirs.api.service.CandidaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Candidaturas e seleção",
    description =
        "Inscrições, etapas, avaliações, propostas e contratação. Acesso condicionado ao perfil, responsabilidade pela vaga e designação do avaliador.")
@RestController
@RequestMapping("/api/v1/candidaturas")
@RequiredArgsConstructor
public class CandidaturaController {
  private final CandidaturaService candidaturas;

  @Operation(
      summary = "Listar candidaturas",
      operationId = "listarCandidaturas",
      description =
          "Filtra por vaga, etapa e status. A ordenação padrão prioriza maiores notas finais e deixa notas ausentes no fim.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping
  public RespostaPaginada<RespostaCandidatura> listar(
      @RequestParam(required = false) Long vagaId,
      @RequestParam(required = false) Long etapaId,
      @RequestParam(required = false) StatusCandidatura status,
      @PageableDefault(sort = "notaFinal", direction = Sort.Direction.DESC) @ParameterObject
          Pageable paginacao) {
    return candidaturas.listar(vagaId, etapaId, status, paginacao);
  }

  @Operation(
      summary = "Consultar detalhes da candidatura",
      operationId = "consultarDetalhesDaCandidatura",
      description =
          "Inclui histórico e avaliações. Avaliadores designados não recebem a proposta salarial.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/{identificador}")
  public RespostaDetalheCandidatura consultar(@PathVariable Long identificador) {
    return candidaturas.consultar(identificador);
  }

  @Operation(
      summary = "Inscrever candidato na vaga",
      operationId = "inscreverCandidatoNaVaga",
      description =
          "A vaga deve estar publicada e o canal deve estar associado a ela. Não permite inscrição duplicada.")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping
  public ResponseEntity<RespostaCandidatura> cadastrar(
      @Valid @RequestBody RequisicaoCandidatura requisicao) {
    var resposta = candidaturas.cadastrar(requisicao);
    return ResponseEntity.created(URI.create("/api/v1/candidaturas/" + resposta.identificador()))
        .body(resposta);
  }

  @Operation(
      summary = "Movimentar candidatura entre etapas",
      operationId = "movimentarCandidaturaEntreEtapas",
      description =
          "A etapa deve pertencer à mesma vaga. Reprovados, contratados e candidaturas com proposta não podem ser movimentados.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PatchMapping("/{identificador}/etapa")
  public RespostaCandidatura movimentar(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoMovimentacaoCandidatura requisicao) {
    return candidaturas.movimentar(identificador, requisicao);
  }

  @Operation(
      summary = "Decidir aprovação da candidatura",
      operationId = "decidirAprovacaoDaCandidatura",
      description = "Permite apenas EM_AVALIACAO para APROVADO ou REPROVADO. A reprovação é final.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PatchMapping("/{identificador}/status")
  public RespostaCandidatura decidir(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoDecisaoCandidatura requisicao) {
    return candidaturas.decidir(identificador, requisicao);
  }

  @Operation(
      summary = "Registrar notas por critério",
      operationId = "registrarNotasPorCriterio",
      description =
          "Notas inteiras de 0 a 5. Critérios devem pertencer à vaga. A nota final normalizada é recalculada e permanece ausente quando a soma é zero.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PutMapping("/{identificador}/avaliacoes")
  public RespostaCandidatura avaliar(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoAvaliacaoCandidatura requisicao) {
    return candidaturas.avaliar(identificador, requisicao);
  }

  @Operation(
      summary = "Designar avaliador para candidatura",
      operationId = "designarAvaliadorParaCandidatura")
  @ApiResponse(
      responseCode = "204",
      description = "Operação concluída, sem corpo de resposta.",
      content = @Content)
  @PostMapping("/{identificador}/avaliadores")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void designar(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoDesignacaoAvaliador requisicao) {
    candidaturas.designar(identificador, requisicao.usuarioId(), false);
  }

  @Operation(
      summary = "Remover designação de avaliador",
      operationId = "removerDesignacaoDeAvaliador")
  @ApiResponse(
      responseCode = "204",
      description = "Operação concluída, sem corpo de resposta.",
      content = @Content)
  @DeleteMapping("/{identificador}/avaliadores/{usuarioId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removerDesignacao(@PathVariable Long identificador, @PathVariable Long usuarioId) {
    candidaturas.designar(identificador, usuarioId, true);
  }

  @Operation(
      summary = "Registrar proposta salarial",
      operationId = "registrarPropostaSalarial",
      description =
          "Exige candidatura aprovada na última etapa. Cada candidatura possui no máximo uma proposta.")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/{identificador}/proposta")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaProposta oferecer(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoProposta requisicao) {
    return candidaturas.oferecer(identificador, requisicao);
  }

  @Operation(
      summary = "Registrar contratação",
      operationId = "registrarContratacao",
      description =
          "Exige aprovação na última etapa, proposta e data de aceite válida. O preenchimento das posições é protegido contra concorrência.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/{identificador}/contratacao")
  public RespostaCandidatura contratar(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoContratacao requisicao) {
    return candidaturas.contratar(identificador, requisicao);
  }
}
