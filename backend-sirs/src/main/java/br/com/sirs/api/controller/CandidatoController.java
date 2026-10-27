package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoCandidato;
import br.com.sirs.api.model.response.RespostaCandidato;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.service.CandidatoService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidatos")
@RequiredArgsConstructor
public class CandidatoController {
  private final CandidatoService candidatos;

  @GetMapping
  public RespostaPaginada<RespostaCandidato> listar(
      @RequestParam(required = false) String pesquisa, Pageable paginacao) {
    return candidatos.listar(pesquisa, paginacao);
  }

  @GetMapping("/{identificador}")
  public RespostaCandidato consultar(@PathVariable Long identificador) {
    return candidatos.consultar(identificador);
  }

  @PostMapping
  public ResponseEntity<RespostaCandidato> cadastrar(
      @Valid @RequestBody RequisicaoCandidato requisicao) {
    var resposta = candidatos.cadastrar(requisicao);
    return ResponseEntity.created(URI.create("/api/v1/candidatos/" + resposta.identificador()))
        .body(resposta);
  }

  @PutMapping("/{identificador}")
  public RespostaCandidato atualizar(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoCandidato requisicao) {
    return candidatos.atualizar(identificador, requisicao);
  }
}
