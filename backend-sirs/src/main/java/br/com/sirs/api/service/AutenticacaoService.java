package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.request.RequisicaoAlteracaoSenha;
import br.com.sirs.api.model.request.RequisicaoAutenticacao;
import br.com.sirs.api.model.response.RespostaAutenticacao;
import br.com.sirs.api.model.response.RespostaUsuario;
import br.com.sirs.api.repository.SessaoSupabaseRepository;
import br.com.sirs.api.repository.UsuarioRepository;
import br.com.sirs.api.security.ContextoAcesso;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {
  private final UsuarioRepository usuarios;
  private final PasswordEncoder codificadorSenha;
  private final JwtEncoder codificadorToken;
  private final Clock relogio;
  private final ContextoAcesso contextoAcesso;
  private final RegistroAuditoriaService registroAuditoria;
  private final ObjectProvider<AutenticacaoSupabaseService> supabase;
  private final SessaoSupabaseRepository sessoesSupabase;

  @Value("${sirs.jwt.issuer}")
  private String emissor;

  @Value("${sirs.jwt.ttl}")
  private Duration validadeToken;

  // A hash with the configured cost keeps nonexistent-user checks computationally comparable.
  private final String hashSenhaInexistente =
      new BCryptPasswordEncoder(12).encode("dummy-password-never-used");

  @Transactional(noRollbackFor = BadCredentialsException.class)
  public RespostaAutenticacao login(RequisicaoAutenticacao requisicao) {
    var usuario = usuarios.lockByEmail(normalizarEmail(requisicao.email())).orElse(null);
    if (usuario == null || !usuario.isAtivo() || usuario.isBloqueado()) {
      codificadorSenha.matches("verificacao-invalida", hashSenhaInexistente);
      throw new BadCredentialsException("Login invalido");
    }
    var externo = supabase.getIfAvailable();
    if (externo != null) {
      try {
        var resposta =
            externo.autenticar(usuario, normalizarEmail(requisicao.email()), requisicao.senha());
        usuario.setTentativasFalhas(0);
        return resposta;
      } catch (BadCredentialsException excecao) {
        registrarFalha(usuario);
        throw excecao;
      }
    }
    boolean tamanhoValido = requisicao.senha().getBytes(StandardCharsets.UTF_8).length <= 72;
    if (!tamanhoValido || !codificadorSenha.matches(requisicao.senha(), usuario.getSenhaHash())) {
      registrarFalha(usuario);
      throw new BadCredentialsException("Login invalido");
    }
    usuario.setTentativasFalhas(0);
    var agora = relogio.instant();
    var expiracao = agora.plus(validadeToken);
    var declaracoes =
        JwtClaimsSet.builder()
            .issuer(emissor)
            .subject(usuario.getId().toString())
            .issuedAt(agora)
            .expiresAt(expiracao)
            .claim("ver", usuario.getTokenVersion())
            .build();
    var token =
        codificadorToken
            .encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), declaracoes))
            .getTokenValue();
    return new RespostaAutenticacao(token, "Bearer", expiracao, RespostaUsuario.of(usuario), null);
  }

  private void registrarFalha(br.com.sirs.api.model.entity.Usuario usuario) {
    usuario.setTentativasFalhas(usuario.getTentativasFalhas() + 1);
    if (usuario.getTentativasFalhas() >= 5) {
      usuario.setBloqueado(true);
      usuario.setTokenVersion(usuario.getTokenVersion() + 1);
      sessoesSupabase.revogarTodas(usuario.getId(), relogio.instant());
    }
  }

  @Transactional
  public RespostaAutenticacao renovarSessao(String refreshToken) {
    var externo = supabase.getIfAvailable();
    if (externo == null)
      throw new RegraNegocioException("Renovacao de sessao disponivel somente com Supabase Auth.");
    return externo.renovar(refreshToken);
  }

  @Transactional
  public void password(RequisicaoAlteracaoSenha requisicao) {
    var usuario = usuarios.lockById(contextoAcesso.identificador()).orElseThrow();
    if (requisicao.novaSenha().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new RegraNegocioException("Senha excede 72 bytes UTF-8.");
    var externo = supabase.getIfAvailable();
    if (externo != null)
      externo.alterarSenha(usuario, requisicao.senhaAtual(), requisicao.novaSenha());
    else {
      if (requisicao.senhaAtual().getBytes(StandardCharsets.UTF_8).length > 72
          || !codificadorSenha.matches(requisicao.senhaAtual(), usuario.getSenhaHash()))
        throw new BadCredentialsException("Senha invalida");
      usuario.setSenhaHash(codificadorSenha.encode(requisicao.novaSenha()));
    }
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    sessoesSupabase.revogarTodas(usuario.getId(), relogio.instant());
    registroAuditoria.registrarAuditoria("usuario", usuario.getId(), "ALTERACAO");
  }

  @Transactional
  public void logout() {
    var usuario = usuarios.lockById(contextoAcesso.identificador()).orElseThrow();
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    sessoesSupabase.revogarTodas(usuario.getId(), relogio.instant());
    var externo = supabase.getIfAvailable();
    if (externo != null)
      externo.encerrarRemotamente(
          ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication())
              .getToken()
              .getTokenValue());
  }

  public static String normalizarEmail(String value) {
    return value.strip().toLowerCase(Locale.ROOT);
  }
}
