package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoAlteracaoSenha;
import br.com.sirs.api.model.request.RequisicaoAutenticacao;
import br.com.sirs.api.model.response.RespostaAutenticacao;
import br.com.sirs.api.model.response.RespostaUsuario;
import br.com.sirs.api.security.ContextoAcesso;
import br.com.sirs.api.service.AutenticacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AutenticacaoController {
  private final AutenticacaoService autenticacao;
  private final ContextoAcesso contextoAcesso;

  @PostMapping("/auth/login")
  public RespostaAutenticacao login(@Valid @RequestBody RequisicaoAutenticacao requisicao) {
    return autenticacao.login(requisicao);
  }

  @GetMapping("/auth/me")
  public RespostaUsuario me() {
    return RespostaUsuario.of(contextoAcesso.usuarioAtual());
  }

  @PostMapping("/auth/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout() {
    autenticacao.logout();
  }

  @PutMapping("/auth/senha")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(@Valid @RequestBody RequisicaoAlteracaoSenha requisicao) {
    autenticacao.password(requisicao);
  }
}
