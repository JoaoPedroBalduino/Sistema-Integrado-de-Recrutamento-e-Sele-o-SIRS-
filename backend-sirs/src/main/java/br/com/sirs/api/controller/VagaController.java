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
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vagas")
@RequiredArgsConstructor
public class VagaController {
  private final VagaService vagas;

  @GetMapping
  public RespostaPaginada<RespostaVaga> listar(
      @RequestParam(required = false) StatusVaga status,
      @RequestParam(required = false) Long areaId,
      @RequestParam(required = false) Long cargoId,
      @RequestParam(required = false) Long recrutadorId,
      @RequestParam(required = false) LocalDate de,
      @RequestParam(required = false) LocalDate ate,
      @PageableDefault(sort = "id") Pageable paginacao) {
    return vagas.listar(status, areaId, cargoId, recrutadorId, de, ate, paginacao);
  }

  @GetMapping("/{identificador}")
  public RespostaDetalheVaga detalhe(@PathVariable Long identificador) {
    return vagas.detalhe(identificador);
  }

  @PostMapping
  public ResponseEntity<RespostaVaga> create(@Valid @RequestBody RequisicaoVaga requisicao) {
    var vaga = vagas.cadastrar(requisicao);
    return ResponseEntity.created(URI.create("/api/v1/vagas/" + vaga.identificador())).body(vaga);
  }

  @PutMapping("/{identificador}")
  public RespostaVaga update(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoVaga requisicao) {
    return vagas.atualizar(identificador, requisicao);
  }

  @PatchMapping("/{identificador}/status")
  public RespostaVaga status(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoAlteracaoStatusVaga requisicao) {
    return vagas.alterarStatus(identificador, requisicao);
  }

  @PostMapping("/{identificador}/etapas")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaEtapaVaga etapa(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoCadastroEtapa requisicao) {
    return vagas.cadastrarEtapa(identificador, requisicao);
  }

  @PostMapping("/{identificador}/criterios")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaCriterioVaga criterio(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoCadastroCriterio requisicao) {
    return vagas.cadastrarCriterio(identificador, requisicao);
  }

  @DeleteMapping("/{identificador}/etapas/{etapaId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluirEtapa(@PathVariable Long identificador, @PathVariable Long etapaId) {
    vagas.excluirEtapa(identificador, etapaId);
  }

  @DeleteMapping("/{identificador}/criterios/{criterioId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluirCriterio(@PathVariable Long identificador, @PathVariable Long criterioId) {
    vagas.excluirCriterio(identificador, criterioId);
  }

  @PostMapping("/{identificador}/horas")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaApontamentoHoras horas(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoApontamentoHoras requisicao) {
    return vagas.registrarHoras(identificador, requisicao);
  }

  @PutMapping("/{identificador}/etapas/{etapaId}")
  public RespostaEtapaVaga atualizarEtapa(
      @PathVariable Long identificador,
      @PathVariable Long etapaId,
      @Valid @RequestBody RequisicaoCadastroEtapa requisicao) {
    return vagas.atualizarEtapa(identificador, etapaId, requisicao);
  }

  @PutMapping("/{identificador}/criterios/{criterioId}")
  public RespostaCriterioVaga atualizarCriterio(
      @PathVariable Long identificador,
      @PathVariable Long criterioId,
      @Valid @RequestBody RequisicaoCadastroCriterio requisicao) {
    return vagas.atualizarCriterio(identificador, criterioId, requisicao);
  }
}
