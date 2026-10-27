package br.com.sirs.api.config;

import br.com.sirs.api.security.ConversorAutenticacaoJwt;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
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
  JwtDecoder jwtDecoder(
      SecretKeySpec chave,
      @Value("${sirs.jwt.issuer}") String emissor,
      ObjectProvider<PropriedadesSupabase> configuracaoSupabase) {
    var propriedades = configuracaoSupabase.getIfAvailable();
    if (propriedades != null) {
      var transporte =
          new JdkClientHttpRequestFactory(
              HttpClient.newBuilder()
                  .connectTimeout(Duration.ofSeconds(5))
                  .followRedirects(HttpClient.Redirect.NEVER)
                  .build());
      transporte.setReadTimeout(Duration.ofSeconds(10));
      var decoder =
          NimbusJwtDecoder.withJwkSetUri(propriedades.getJwksUrl().toString())
              .jwsAlgorithms(
                  algoritmos -> {
                    algoritmos.clear();
                    algoritmos.add(SignatureAlgorithm.ES256);
                    algoritmos.add(SignatureAlgorithm.RS256);
                  })
              .restOperations(new RestTemplate(transporte))
              .build();
      OAuth2TokenValidator<Jwt> audiencia =
          token ->
              token.getAudience() != null && token.getAudience().contains("authenticated")
                  ? OAuth2TokenValidatorResult.success()
                  : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
      decoder.setJwtValidator(
          new DelegatingOAuth2TokenValidator<>(
              JwtValidators.createDefaultWithIssuer(propriedades.emissor()), audiencia));
      return decoder;
    }
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
  SecurityFilterChain security(HttpSecurity segurancaHttp, ConversorAutenticacaoJwt conversor)
      throws Exception {
    return segurancaHttp
        .csrf(configuracao -> configuracao.disable())
        .cors(configuracao -> {})
        .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            autorizacao ->
                autorizacao
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh")
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
                            configuracaoToken.jwtAuthenticationConverter(conversor)))
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
