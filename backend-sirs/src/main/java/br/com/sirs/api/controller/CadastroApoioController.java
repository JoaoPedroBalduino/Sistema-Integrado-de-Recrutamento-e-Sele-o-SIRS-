package br.com.sirs.api.controller;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.model.enums.TipoCadastroApoio;
import br.com.sirs.api.model.request.RequisicaoCadastroApoio;
import br.com.sirs.api.model.response.RespostaCadastroApoio;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.service.CadastroApoioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cadastros/{tipo}")
@RequiredArgsConstructor
public class CadastroApoioController {
  private final CadastroApoioService cadastros;

  private TipoCadastroApoio tipo(String tipo) {
    try {
      return TipoCadastroApoio.valueOf(tipo.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException excecao) {
      throw new RecursoNaoEncontradoException("Cadastro", tipo);
    }
  }

  @GetMapping
  public RespostaPaginada<RespostaCadastroApoio> list(
      @PathVariable String tipo, Pageable paginacao) {
    return cadastros.list(tipo(tipo), paginacao);
  }

  @PostMapping
  public ResponseEntity<RespostaCadastroApoio> create(
      @PathVariable String tipo, @Valid @RequestBody RequisicaoCadastroApoio requisicao) {
    var cadastro = cadastros.save(tipo(tipo), null, requisicao);
    return ResponseEntity.created(
            URI.create("/api/v1/cadastros/" + tipo + "/" + cadastro.identificador()))
        .body(cadastro);
  }

  @PutMapping("/{identificador}")
  public RespostaCadastroApoio update(
      @PathVariable String tipo,
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoCadastroApoio requisicao) {
    return cadastros.save(tipo(tipo), identificador, requisicao);
  }
}
