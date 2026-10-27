package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoAlteracaoSenha;
import br.com.sirs.api.model.request.RequisicaoAutenticacao;
import br.com.sirs.api.model.response.RespostaAutenticacao;
import br.com.sirs.api.model.response.RespostaUsuario;
import br.com.sirs.api.security.ContextoAcesso;
import br.com.sirs.api.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Login e gerenciamento da sessão do usuário autenticado.")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AutenticacaoController {
  private final AutenticacaoService autenticacao;
  private final ContextoAcesso contextoAcesso;

  @Operation(
      summary = "Autenticar usuário",
      operationId = "autenticarUsuario",
      description =
          "Operação pública. Retorna um accessToken JWT válido por 30 minutos. Cinco falhas consecutivas bloqueiam a conta.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/auth/login")
  public RespostaAutenticacao login(@Valid @RequestBody RequisicaoAutenticacao requisicao) {
    return autenticacao.login(requisicao);
  }

  @Operation(summary = "Consultar usuário autenticado", operationId = "consultarUsuarioAutenticado")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/auth/me")
  public RespostaUsuario me() {
    return RespostaUsuario.of(contextoAcesso.usuarioAtual());
  }

  @Operation(
      summary = "Encerrar sessões do usuário",
      operationId = "encerrarSessoesDoUsuario",
      description = "Revoga todos os tokens emitidos anteriormente para esta conta.")
  @ApiResponse(
      responseCode = "204",
      description = "Operação concluída, sem corpo de resposta.",
      content = @Content)
  @PostMapping("/auth/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout() {
    autenticacao.logout();
  }

  @Operation(
      summary = "Alterar senha do usuário",
      operationId = "alterarSenhaDoUsuario",
      description = "Exige a senha atual e revoga os tokens anteriores após a alteração.")
  @ApiResponse(
      responseCode = "204",
      description = "Operação concluída, sem corpo de resposta.",
      content = @Content)
  @PutMapping("/auth/senha")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(@Valid @RequestBody RequisicaoAlteracaoSenha requisicao) {
    autenticacao.password(requisicao);
  }
}
