package br.com.sirs.api.config;

import br.com.sirs.api.repository.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class ConfiguracaoSeguranca {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecretKeySpec jwtKey(@Value("${sirs.jwt.secret}") String segredo) {
    byte[] conteudoSegredo = segredo.getBytes(StandardCharsets.UTF_8);
    if (conteudoSegredo.length < 32)
      throw new IllegalArgumentException("JWT_SECRET deve ter ao menos 32 bytes.");
    return new SecretKeySpec(conteudoSegredo, "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKeySpec chave) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKeySpec chave, @Value("${sirs.jwt.issuer}") String emissor) {
    var decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(emissor));
    return decoder;
  }

  @Bean
  CorsConfigurationSource cors(@Value("${sirs.cors.origins}") List<String> origensPermitidas) {
    var configuracao = new CorsConfiguration();
    configuracao.setAllowedOrigins(origensPermitidas);
    configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuracao.setExposedHeaders(List.of("Location", "X-Request-Id"));
    configuracao.setAllowCredentials(false);
    configuracao.setMaxAge(3600L);
    var origem = new UrlBasedCorsConfigurationSource();
    origem.registerCorsConfiguration("/**", configuracao);
    return origem;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity segurancaHttp, UsuarioRepository usuarios)
      throws Exception {
    return segurancaHttp
        .csrf(configuracao -> configuracao.disable())
        .cors(configuracao -> {})
        .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            autorizacao ->
                autorizacao
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers(
                        "/actuator/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            tratamentoExcecao ->
                tratamentoExcecao
                    .authenticationEntryPoint(
                        (requisicao, resposta, excecao) ->
                            error(resposta, 401, "Autenticacao necessaria."))
                    .accessDeniedHandler(
                        (requisicao, resposta, excecao) -> error(resposta, 403, "Acesso negado.")))
        .oauth2ResourceServer(
            servidorRecursos ->
                servidorRecursos
                    .authenticationEntryPoint(
                        (requisicao, resposta, excecao) ->
                            error(resposta, 401, "Token invalido ou expirado."))
                    .jwt(
                        configuracaoToken ->
                            configuracaoToken.jwtAuthenticationConverter(
                                jwt -> {
                                  try {
                                    var usuario =
                                        usuarios
                                            .findById(Long.valueOf(jwt.getSubject()))
                                            .orElseThrow();
                                    Number versaoToken = jwt.getClaim("ver");
                                    if (!usuario.isAtivo()
                                        || usuario.isBloqueado()
                                        || versaoToken == null
                                        || versaoToken.longValue() != usuario.getTokenVersion())
                                      throw new IllegalArgumentException();
                                    return new JwtAuthenticationToken(
                                        jwt,
                                        List.of(
                                            new SimpleGrantedAuthority(
                                                "ROLE_" + usuario.getPerfil())),
                                        jwt.getSubject());
                                  } catch (RuntimeException excecao) {
                                    throw new OAuth2AuthenticationException(
                                        new OAuth2Error("invalid_token"));
                                  }
                                })))
        .build();
  }

  private static void error(HttpServletResponse resposta, int status, String detalhe)
      throws IOException {
    resposta.setStatus(status);
    resposta.setContentType("application/problem+json");
    resposta.setCharacterEncoding("UTF-8");
    if (status == 401) resposta.setHeader("WWW-Authenticate", "Bearer");
    resposta
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"status\":" + status + ",\"detail\":\"" + detalhe + "\"}");
  }
}
