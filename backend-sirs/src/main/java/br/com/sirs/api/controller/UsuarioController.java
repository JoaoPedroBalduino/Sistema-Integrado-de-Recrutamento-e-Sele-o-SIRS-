package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.*;
import br.com.sirs.api.model.response.*;
import br.com.sirs.api.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
  private final UsuarioService usuarios;

  @GetMapping
  public RespostaPaginada<RespostaUsuario> listar(Pageable paginacao) {
    return usuarios.listarUsuarios(paginacao);
  }

  @PostMapping
  public ResponseEntity<RespostaUsuario> cadastrar(
      @Valid @RequestBody RequisicaoCadastroUsuario requisicao) {
    var resposta = usuarios.cadastrar(requisicao);
    return ResponseEntity.created(URI.create("/api/v1/usuarios/" + resposta.identificador()))
        .body(resposta);
  }

  @PutMapping("/{identificador}")
  public RespostaUsuario atualizar(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoAtualizacaoUsuario requisicao) {
    return usuarios.atualizar(identificador, requisicao);
  }
}
