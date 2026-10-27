package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoSolicitacaoTitular;
import br.com.sirs.api.model.response.RespostaExportacaoTitular;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.model.response.RespostaSolicitacaoTitular;
import br.com.sirs.api.service.PrivacidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Privacidade",
    description = "Solicitações de titular com prazo de 15 dias e exportação dos dados pessoais.")
@RestController
@RequestMapping("/api/v1/privacidade")
@RequiredArgsConstructor
public class PrivacidadeController {
  private final PrivacidadeService privacidade;

  @Operation(
      summary = "Listar solicitações de titular",
      operationId = "listarSolicitacoesDeTitular",
      description = "Disponível para ADMIN e COORD_RH.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/solicitacoes")
  public RespostaPaginada<RespostaSolicitacaoTitular> listar(@ParameterObject Pageable paginacao) {
    return privacidade.listar(paginacao);
  }

  @Operation(
      summary = "Registrar solicitação de titular",
      operationId = "registrarSolicitacaoDeTitular",
      description = "Somente ADMIN. Tipos: ACESSO, CORRECAO e ANONIMIZACAO.")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/solicitacoes")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaSolicitacaoTitular cadastrar(
      @Valid @RequestBody RequisicaoSolicitacaoTitular requisicao) {
    return privacidade.cadastrar(requisicao);
  }

  @Operation(
      summary = "Concluir solicitação de titular",
      operationId = "concluirSolicitacaoDeTitular",
      description =
          "Somente ADMIN. Pedidos de anonimização removem os dados pessoais quando não há candidaturas ativas ou contratações.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/solicitacoes/{identificador}/conclusao")
  public RespostaSolicitacaoTitular concluir(@PathVariable Long identificador) {
    return privacidade.concluir(identificador);
  }

  @Operation(
      summary = "Exportar dados do candidato",
      operationId = "exportarDadosDoCandidato",
      description = "Somente ADMIN. A consulta é registrada na trilha de auditoria.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/candidatos/{candidatoId}/exportacao")
  public RespostaExportacaoTitular exportar(@PathVariable Long candidatoId) {
    return privacidade.exportar(candidatoId);
  }
}
