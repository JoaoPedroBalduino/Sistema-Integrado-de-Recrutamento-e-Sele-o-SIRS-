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
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidaturas")
@RequiredArgsConstructor
public class CandidaturaController {
  private final CandidaturaService candidaturas;

  @GetMapping
  public RespostaPaginada<RespostaCandidatura> listar(
      @RequestParam(required = false) Long vagaId,
      @RequestParam(required = false) Long etapaId,
      @RequestParam(required = false) StatusCandidatura status,
      @PageableDefault(sort = "notaFinal", direction = Sort.Direction.DESC) Pageable paginacao) {
    return candidaturas.listar(vagaId, etapaId, status, paginacao);
  }

  @GetMapping("/{identificador}")
  public RespostaDetalheCandidatura consultar(@PathVariable Long identificador) {
    return candidaturas.consultar(identificador);
  }

  @PostMapping
  public ResponseEntity<RespostaCandidatura> cadastrar(
      @Valid @RequestBody RequisicaoCandidatura requisicao) {
    var resposta = candidaturas.cadastrar(requisicao);
    return ResponseEntity.created(URI.create("/api/v1/candidaturas/" + resposta.identificador()))
        .body(resposta);
  }

  @PatchMapping("/{identificador}/etapa")
  public RespostaCandidatura movimentar(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoMovimentacaoCandidatura requisicao) {
    return candidaturas.movimentar(identificador, requisicao);
  }

  @PatchMapping("/{identificador}/status")
  public RespostaCandidatura decidir(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoDecisaoCandidatura requisicao) {
    return candidaturas.decidir(identificador, requisicao);
  }

  @PutMapping("/{identificador}/avaliacoes")
  public RespostaCandidatura avaliar(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoAvaliacaoCandidatura requisicao) {
    return candidaturas.avaliar(identificador, requisicao);
  }

  @PostMapping("/{identificador}/avaliadores")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void designar(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoDesignacaoAvaliador requisicao) {
    candidaturas.designar(identificador, requisicao.usuarioId(), false);
  }

  @DeleteMapping("/{identificador}/avaliadores/{usuarioId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removerDesignacao(@PathVariable Long identificador, @PathVariable Long usuarioId) {
    candidaturas.designar(identificador, usuarioId, true);
  }

  @PostMapping("/{identificador}/proposta")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaProposta oferecer(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoProposta requisicao) {
    return candidaturas.oferecer(identificador, requisicao);
  }

  @PostMapping("/{identificador}/contratacao")
  public RespostaCandidatura contratar(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoContratacao requisicao) {
    return candidaturas.contratar(identificador, requisicao);
  }
}
