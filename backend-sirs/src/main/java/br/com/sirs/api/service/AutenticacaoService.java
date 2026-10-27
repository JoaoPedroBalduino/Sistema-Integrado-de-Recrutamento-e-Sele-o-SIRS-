package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.request.RequisicaoAlteracaoSenha;
import br.com.sirs.api.model.request.RequisicaoAutenticacao;
import br.com.sirs.api.model.response.RespostaAutenticacao;
import br.com.sirs.api.model.response.RespostaUsuario;
import br.com.sirs.api.repository.UsuarioRepository;
import br.com.sirs.api.security.ContextoAcesso;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
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
    boolean tamanhoSenhaValido = requisicao.senha().getBytes(StandardCharsets.UTF_8).length <= 72;
    boolean senhaCorreta =
        tamanhoSenhaValido
            && codificadorSenha.matches(
                requisicao.senha(),
                usuario == null ? hashSenhaInexistente : usuario.getSenhaHash());
    if (usuario == null || !usuario.isAtivo() || usuario.isBloqueado())
      throw new BadCredentialsException("Login invalido");
    if (!senhaCorreta) {
      usuario.setTentativasFalhas(usuario.getTentativasFalhas() + 1);
      if (usuario.getTentativasFalhas() >= 5) {
        usuario.setBloqueado(true);
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
      }
      throw new BadCredentialsException("Login invalido");
    }
    usuario.setTentativasFalhas(0);
    var instanteAtual = relogio.instant();
    var expiracao = instanteAtual.plus(validadeToken);
    var declaracoesToken =
        JwtClaimsSet.builder()
            .issuer(emissor)
            .subject(usuario.getId().toString())
            .issuedAt(instanteAtual)
            .expiresAt(expiracao)
            .claim("ver", usuario.getTokenVersion())
            .build();
    var token =
        codificadorToken
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(), declaracoesToken))
            .getTokenValue();
    return new RespostaAutenticacao(token, "Bearer", expiracao, RespostaUsuario.of(usuario));
  }

  @Transactional
  public void password(RequisicaoAlteracaoSenha requisicao) {
    var usuario = usuarios.lockById(contextoAcesso.identificador()).orElseThrow();
    if (requisicao.senhaAtual().getBytes(StandardCharsets.UTF_8).length > 72
        || !codificadorSenha.matches(requisicao.senhaAtual(), usuario.getSenhaHash()))
      throw new BadCredentialsException("Senha invalida");
    if (requisicao.novaSenha().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new RegraNegocioException("Senha excede 72 bytes UTF-8.");
    usuario.setSenhaHash(codificadorSenha.encode(requisicao.novaSenha()));
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    registroAuditoria.registrarAuditoria("usuario", usuario.getId(), "ALTERACAO");
  }

  @Transactional
  public void logout() {
    var usuario = usuarios.lockById(contextoAcesso.identificador()).orElseThrow();
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
  }

  public static String normalizarEmail(String value) {
    return value.strip().toLowerCase(Locale.ROOT);
  }
}
