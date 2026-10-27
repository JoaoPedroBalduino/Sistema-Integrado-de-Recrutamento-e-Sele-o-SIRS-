package br.com.sirs.api.security;

import br.com.sirs.api.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversorAutenticacaoJwt implements Converter<Jwt, JwtAuthenticationToken> {
  private final UsuarioRepository usuarios;
  private final SessaoSupabaseRepository sessoes;

  @Value("${sirs.auth.provider:local}")
  private String provedor;

  @Override
  public JwtAuthenticationToken convert(Jwt token) {
    try {
      br.com.sirs.api.model.entity.Usuario usuario;
      if ("supabase".equals(provedor)) {
        if (token.getExpiresAt() == null || token.getIssuedAt() == null)
          throw new IllegalArgumentException();
        usuario =
            usuarios.findBySupabaseUsuarioId(UUID.fromString(token.getSubject())).orElseThrow();
        var sessao =
            sessoes
                .buscarAtiva(UUID.fromString(token.getClaimAsString("session_id")))
                .orElseThrow();
        if (!sessao.getUsuario().getId().equals(usuario.getId()))
          throw new IllegalArgumentException();
      } else {
        usuario = usuarios.findById(Long.valueOf(token.getSubject())).orElseThrow();
        Number versao = token.getClaim("ver");
        if (versao == null || versao.longValue() != usuario.getTokenVersion())
          throw new IllegalArgumentException();
      }
      if (!usuario.isAtivo() || usuario.isBloqueado()) throw new IllegalArgumentException();
      return new JwtAuthenticationToken(
          token,
          List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil())),
          usuario.getId().toString());
    } catch (RuntimeException excecao) {
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
    }
  }
}
