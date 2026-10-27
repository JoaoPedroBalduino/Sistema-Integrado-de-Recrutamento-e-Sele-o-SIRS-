package br.com.sirs.api.config;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.oauth2.jwt.*;
import tools.jackson.databind.json.JsonMapper;

class ValidacaoJwtSupabaseTest {
  private static RSAKey chaveRsa;
  private static ECKey chaveEc;
  private HttpServer servidor;
  private JwtDecoder decoder;
  private String emissor;

  @BeforeAll
  static void gerarChaves() throws Exception {
    chaveRsa = new RSAKeyGenerator(2048).keyID("rsa-teste").generate();
    chaveEc = new ECKeyGenerator(Curve.P_256).keyID("ec-teste").generate();
  }

  @BeforeEach
  void preparar() throws Exception {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var dados =
        new JsonMapper()
            .writeValueAsBytes(
                new JWKSet(List.of(chaveRsa.toPublicJWK(), chaveEc.toPublicJWK())).toJSONObject());
    servidor.createContext(
        "/auth/v1/.well-known/jwks.json",
        requisicao -> {
          requisicao.getResponseHeaders().set("Content-Type", "application/json");
          requisicao.sendResponseHeaders(200, dados.length);
          requisicao.getResponseBody().write(dados);
          requisicao.close();
        });
    servidor.start();
    String endereco = "http://127.0.0.1:" + servidor.getAddress().getPort();
    var propriedades = new PropriedadesSupabase();
    propriedades.setUrl(URI.create(endereco));
    propriedades.setJwksUrl(URI.create(endereco + "/auth/v1/.well-known/jwks.json"));
    emissor = propriedades.emissor();
    var beans = new StaticListableBeanFactory();
    beans.addBean("supabase", propriedades);
    decoder =
        new ConfiguracaoSeguranca()
            .jwtDecoder(
                new SecretKeySpec(
                    "chave-local-apenas-dos-testes-com-32-bytes".getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"),
                "sirs-api",
                beans.getBeanProvider(PropriedadesSupabase.class));
  }

  @AfterEach
  void encerrar() {
    servidor.stop(0);
  }

  private String assinar(String issuer, String audience, Instant validade, boolean curva)
      throws Exception {
    var declaracoes =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(audience)
            .subject(UUID.randomUUID().toString())
            .issueTime(Date.from(Instant.now().minusSeconds(30)))
            .expirationTime(Date.from(validade))
            .claim("session_id", UUID.randomUUID().toString())
            .build();
    var token =
        new SignedJWT(
            new JWSHeader.Builder(curva ? JWSAlgorithm.ES256 : JWSAlgorithm.RS256)
                .keyID(curva ? chaveEc.getKeyID() : chaveRsa.getKeyID())
                .build(),
            declaracoes);
    token.sign(curva ? new ECDSASigner(chaveEc) : new RSASSASigner(chaveRsa));
    return token.serialize();
  }

  @Test
  void validaAssinaturaRsaComChavePublica() throws Exception {
    assertNotNull(
        decoder.decode(assinar(emissor, "authenticated", Instant.now().plusSeconds(300), false)));
  }

  @Test
  void validaAssinaturaEs256UsadaPeloProjeto() throws Exception {
    assertNotNull(
        decoder.decode(assinar(emissor, "authenticated", Instant.now().plusSeconds(300), true)));
  }

  @Test
  void rejeitaEmissorDeOutroProjeto() throws Exception {
    var token =
        assinar(
            "https://outro.supabase.test/auth/v1",
            "authenticated",
            Instant.now().plusSeconds(300),
            false);
    assertThrows(JwtException.class, () -> decoder.decode(token));
  }

  @Test
  void rejeitaAudienciaAdministrativa() throws Exception {
    var token = assinar(emissor, "service_role", Instant.now().plusSeconds(300), false);
    assertThrows(JwtException.class, () -> decoder.decode(token));
  }

  @Test
  void rejeitaTokenExpirado() throws Exception {
    var token = assinar(emissor, "authenticated", Instant.now().minusSeconds(300), false);
    assertThrows(JwtException.class, () -> decoder.decode(token));
  }

  @Test
  void rejeitaAssinaturaDeOutraChave() throws Exception {
    var token =
        SignedJWT.parse(assinar(emissor, "authenticated", Instant.now().plusSeconds(300), false));
    token = new SignedJWT(token.getHeader(), token.getJWTClaimsSet());
    token.sign(new RSASSASigner(new RSAKeyGenerator(2048).generate()));
    String assinaturaInvalida = token.serialize();
    assertThrows(JwtException.class, () -> decoder.decode(assinaturaInvalida));
  }
}
