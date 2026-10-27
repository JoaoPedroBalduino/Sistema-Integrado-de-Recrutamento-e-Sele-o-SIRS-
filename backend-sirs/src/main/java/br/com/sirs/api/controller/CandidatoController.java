package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoCandidato;
import br.com.sirs.api.model.response.RespostaCandidato;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.service.CandidatoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Candidatos",
    description =
        "Cadastro de candidatos, currículo e dados de coleta. Gestores consultam somente candidatos vinculados às próprias vagas.")
@RestController
@RequestMapping("/api/v1/candidatos")
@RequiredArgsConstructor
public class CandidatoController {
  private final CandidatoService candidatos;

  @Operation(
      summary = "Pesquisar candidatos",
      operationId = "pesquisarCandidatos",
      description = "Pesquisa por nome ou e-mail, com paginação.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping
  public RespostaPaginada<RespostaCandidato> listar(
      @RequestParam(required = false) String pesquisa, @ParameterObject Pageable paginacao) {
    return candidatos.listar(pesquisa, paginacao);
  }

  @Operation(summary = "Consultar candidato", operationId = "consultarCandidato")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/{identificador}")
  public RespostaCandidato consultar(@PathVariable Long identificador) {
    return candidatos.consultar(identificador);
  }

  @Operation(
      summary = "Cadastrar candidato",
      operationId = "cadastrarCandidato",
      description =
          "Exige COORD_RH ou RECRUTADOR, base legal e data de coleta. Consentimento exige versão do termo.")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping
  public ResponseEntity<RespostaCandidato> cadastrar(
      @Valid @RequestBody RequisicaoCandidato requisicao) {
    var resposta = candidatos.cadastrar(requisicao);
    return ResponseEntity.created(URI.create("/api/v1/candidatos/" + resposta.identificador()))
        .body(resposta);
  }

  @Operation(
      summary = "Atualizar candidato",
      operationId = "atualizarCandidato",
      description =
          "Exige COORD_RH ou RECRUTADOR. Um candidato anonimizado não pode ser reidentificado.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PutMapping("/{identificador}")
  public RespostaCandidato atualizar(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoCandidato requisicao) {
    return candidatos.atualizar(identificador, requisicao);
  }
}
