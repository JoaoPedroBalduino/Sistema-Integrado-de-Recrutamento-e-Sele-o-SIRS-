package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoAlteracaoSenha;
import br.com.sirs.api.model.request.RequisicaoAutenticacao;
import br.com.sirs.api.model.request.RequisicaoRenovacaoSessao;
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

@Tag(
    name = "AutenticaÃ§Ã£o",
    description = "Login e gerenciamento da sessÃ£o do usuÃ¡rio autenticado.")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AutenticacaoController {
  private final AutenticacaoService autenticacao;
  private final ContextoAcesso contextoAcesso;

  @Operation(
      summary = "Autenticar usuÃ¡rio",
      operationId = "autenticarUsuario",
      description =
          "Operacao publica. O modo local usa JWT de 30 minutos; no perfil Supabase, a validade e definida pelo provedor. Cinco falhas consecutivas bloqueiam a conta.")
  @ApiResponse(
      responseCode = "200",
      description = "OperaÃ§Ã£o concluÃ­da com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping("/auth/login")
  public RespostaAutenticacao login(@Valid @RequestBody RequisicaoAutenticacao requisicao) {
    return autenticacao.login(requisicao);
  }

  @Operation(
      summary = "Consultar usuÃ¡rio autenticado",
      operationId = "consultarUsuarioAutenticado")
  @ApiResponse(
      responseCode = "200",
      description = "OperaÃ§Ã£o concluÃ­da com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/auth/me")
  public RespostaUsuario me() {
    return RespostaUsuario.of(contextoAcesso.usuarioAtual());
  }

  @Operation(
      summary = "Encerrar sessÃµes do usuÃ¡rio",
      operationId = "encerrarSessoesDoUsuario",
      description = "Revoga todos os tokens emitidos anteriormente para esta conta.")
  @ApiResponse(
      responseCode = "204",
      description = "OperaÃ§Ã£o concluÃ­da, sem corpo de resposta.",
      content = @Content)
  @PostMapping("/auth/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout() {
    autenticacao.logout();
  }

  @Operation(
      summary = "Alterar senha do usuÃ¡rio",
      operationId = "alterarSenhaDoUsuario",
      description = "Exige a senha atual e revoga os tokens anteriores apÃ³s a alteraÃ§Ã£o.")
  @ApiResponse(
      responseCode = "204",
      description = "OperaÃ§Ã£o concluÃ­da, sem corpo de resposta.",
      content = @Content)
  @PutMapping("/auth/senha")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(@Valid @RequestBody RequisicaoAlteracaoSenha requisicao) {
    autenticacao.password(requisicao);
  }

  @Operation(
      summary = "Renovar sessão Supabase",
      operationId = "renovarSessaoSupabase",
      description =
          "Operação pública: envie o refreshToken no corpo, sem Authorization expirado. Disponível somente no perfil Supabase.")
  @ApiResponse(
      responseCode = "200",
      description = "Sessão renovada e refresh token rotacionado.",
      useReturnTypeSchema = true)
  @PostMapping("/auth/refresh")
  public RespostaAutenticacao renovar(@Valid @RequestBody RequisicaoRenovacaoSessao requisicao) {
    return autenticacao.renovarSessao(requisicao.refreshToken());
  }
}
