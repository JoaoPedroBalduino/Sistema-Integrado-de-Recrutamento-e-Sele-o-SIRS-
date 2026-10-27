package br.com.sirs.api.service;

import br.com.sirs.api.integration.supabase.ClienteSupabase;
import br.com.sirs.api.model.entity.*;
import br.com.sirs.api.model.response.*;
import br.com.sirs.api.repository.*;
import java.time.Clock;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "sirs.auth.provider", havingValue = "supabase")
@RequiredArgsConstructor
@Slf4j
public class AutenticacaoSupabaseService {
  private final ClienteSupabase cliente;
  private final JwtDecoder decoder;
  private final SessaoSupabaseRepository sessoes;
  private final UsuarioRepository usuarios;
  private final Clock relogio;

  public RespostaAutenticacao autenticar(Usuario usuario, String email, String senha) {
    var remota = cliente.autenticar(email, senha);
    var token = validar(remota);
    if (usuario.getSupabaseUsuarioId() == null
        || !usuario.getSupabaseUsuarioId().equals(remota.user().id())) throw invalida();
    var identificadorSessao = UUID.fromString(token.getClaimAsString("session_id"));
    var sessao = sessoes.findById(identificadorSessao).orElse(null);
    if (sessao != null
        && (sessao.getRevogadaEm() != null || !sessao.getUsuario().getId().equals(usuario.getId())))
      throw invalida();
    if (sessao == null) {
      sessao = new SessaoSupabase();
      sessao.setId(identificadorSessao);
      sessao.setUsuario(usuario);
      sessao.setCriadaEm(relogio.instant());
      sessoes.saveAndFlush(sessao);
    }
    return resposta(usuario, remota, token);
  }

  public RespostaAutenticacao renovar(String refreshToken) {
    var remota = cliente.renovar(refreshToken);
    var token = validar(remota);
    var usuario = usuarios.findBySupabaseUsuarioId(remota.user().id()).orElseThrow(this::invalida);
    var sessao =
        sessoes
            .buscarAtiva(UUID.fromString(token.getClaimAsString("session_id")))
            .orElseThrow(this::invalida);
    if (!usuario.isAtivo()
        || usuario.isBloqueado()
        || !sessao.getUsuario().getId().equals(usuario.getId())) throw invalida();
    return resposta(usuario, remota, token);
  }

  public UUID provisionarConta(String email, String senha) {
    ClienteSupabase.UsuarioRemoto usuario;
    try {
      usuario = cliente.criarUsuario(email, senha);
    } catch (br.com.sirs.api.exception.RegraNegocioException excecao) {
      try {
        var sessao = cliente.autenticar(email, senha);
        validar(sessao);
        usuario = sessao.user();
      } catch (BadCredentialsException credenciaisInvalidas) {
        throw excecao;
      }
    }
    if (usuario.id() == null
        || usuario.email() == null
        || !AutenticacaoService.normalizarEmail(email)
            .equals(AutenticacaoService.normalizarEmail(usuario.email()))) throw invalida();
    return usuario.id();
  }

  public void validarVinculo(UUID identificador, String email) {
    var remoto = cliente.consultarUsuario(identificador);
    if (remoto.email() == null
        || !identificador.equals(remoto.id())
        || !AutenticacaoService.normalizarEmail(email)
            .equals(AutenticacaoService.normalizarEmail(remoto.email())))
      throw new br.com.sirs.api.exception.RegraNegocioException(
          "O e-mail da conta Supabase deve coincidir com o cadastro SIRS.");
  }

  public void alterarSenha(Usuario usuario, String senhaAtual, String novaSenha) {
    var sessao = cliente.autenticar(usuario.getEmail(), senhaAtual);
    validar(sessao);
    if (!sessao.user().id().equals(usuario.getSupabaseUsuarioId())) throw invalida();
    cliente.alterarSenha(sessao.accessToken(), novaSenha);
  }

  public void encerrarRemotamente(String accessToken) {
    try {
      cliente.encerrarSessoes(accessToken);
    } catch (br.com.sirs.api.exception.IntegracaoIndisponivelException excecao) {
      log.warn("Supabase Auth indisponivel no logout; sessoes SIRS foram revogadas localmente.");
    }
  }

  private Jwt validar(ClienteSupabase.SessaoRemota sessao) {
    try {
      var token = decoder.decode(sessao.accessToken());
      if (token.getExpiresAt() == null
          || token.getIssuedAt() == null
          || !UUID.fromString(token.getSubject()).equals(sessao.user().id())) throw invalida();
      UUID.fromString(token.getClaimAsString("session_id"));
      return token;
    } catch (JwtException | IllegalArgumentException | NullPointerException excecao) {
      throw invalida();
    }
  }

  private RespostaAutenticacao resposta(
      Usuario usuario, ClienteSupabase.SessaoRemota sessao, Jwt token) {
    return new RespostaAutenticacao(
        sessao.accessToken(),
        "Bearer",
        token.getExpiresAt(),
        RespostaUsuario.of(usuario),
        sessao.refreshToken());
  }

  private BadCredentialsException invalida() {
    return new BadCredentialsException("Conta nao vinculada ou sessao indisponivel.");
  }
}
