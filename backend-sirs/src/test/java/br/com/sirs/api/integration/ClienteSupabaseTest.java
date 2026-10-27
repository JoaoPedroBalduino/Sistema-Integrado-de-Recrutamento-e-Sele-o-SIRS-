package br.com.sirs.api.integration;

import static org.junit.jupiter.api.Assertions.*;

import br.com.sirs.api.config.PropriedadesSupabase;
import br.com.sirs.api.exception.IntegracaoIndisponivelException;
import br.com.sirs.api.integration.supabase.ClienteSupabase;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.RestClient;

class ClienteSupabaseTest {
  private HttpServer servidor;
  private ClienteSupabase cliente;
  private final AtomicReference<String> caminho = new AtomicReference<>();
  private final AtomicReference<String> corpo = new AtomicReference<>();
  private final AtomicReference<String> chave = new AtomicReference<>();
  private final AtomicReference<String> autorizacao = new AtomicReference<>();
  private int statusResposta = 200;
  private String resposta;
  private static final UUID IDENTIFICADOR = UUID.fromString("39470ead-74f2-44ee-9583-7806fd3fbb32");

  @BeforeEach
  void preparar() throws Exception {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    resposta =
        "{\"access_token\":\"access-fixture\",\"refresh_token\":\"refresh-fixture\",\"user\":{\"id\":\""
            + IDENTIFICADOR
            + "\",\"email\":\"conta@example.test\"},\"expires_in\":3600}";
    servidor.createContext(
        "/auth/v1",
        requisicao -> {
          caminho.set(requisicao.getRequestURI().toString());
          corpo.set(new String(requisicao.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          chave.set(requisicao.getRequestHeaders().getFirst("apikey"));
          autorizacao.set(requisicao.getRequestHeaders().getFirst("Authorization"));
          requisicao.getResponseHeaders().set("Content-Type", "application/json");
          byte[] dados = resposta.getBytes(StandardCharsets.UTF_8);
          requisicao.sendResponseHeaders(statusResposta, statusResposta == 204 ? -1 : dados.length);
          if (statusResposta != 204) requisicao.getResponseBody().write(dados);
          requisicao.close();
        });
    servidor.start();
    var propriedades = new PropriedadesSupabase();
    propriedades.setUrl(URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()));
    propriedades.setPublishableKey("publishable-fixture");
    propriedades.setSecretKey("secret-fixture");
    cliente = new ClienteSupabase(propriedades, RestClient.builder());
  }

  @AfterEach
  void encerrar() {
    servidor.stop(0);
  }

  @Test
  void autenticaComChavePublicaSemEnviarChaveAdministrativa() {
    var sessao = cliente.autenticar("conta@example.test", "senha-fixture");
    assertEquals(IDENTIFICADOR, sessao.user().id());
    assertEquals("/auth/v1/token?grant_type=password", caminho.get());
    assertEquals("publishable-fixture", chave.get());
    assertNull(autorizacao.get());
    assertTrue(corpo.get().contains("senha-fixture"));
  }

  @Test
  void renovaTokenPeloFluxoCorreto() {
    cliente.renovar("refresh-fixture");
    assertEquals("/auth/v1/token?grant_type=refresh_token", caminho.get());
    assertTrue(corpo.get().contains("refresh_token"));
  }

  @Test
  void reservaChavePrivadaParaCriacaoAdministrativa() {
    resposta = "{\"id\":\"" + IDENTIFICADOR + "\",\"email\":\"conta@example.test\"}";
    cliente.criarUsuario("conta@example.test", "senha-fixture");
    assertEquals("secret-fixture", chave.get());
    assertEquals("/auth/v1/admin/users", caminho.get());
    assertTrue(corpo.get().contains("email_confirm"));
  }

  @Test
  void trocaSenhaUsandoTokenDaPropriaConta() {
    cliente.alterarSenha("access-fixture", "nova-senha-fixture");
    assertEquals("Bearer access-fixture", autorizacao.get());
    assertEquals("publishable-fixture", chave.get());
    assertEquals("/auth/v1/user", caminho.get());
  }

  @Test
  void encerraSessoesRemotasGlobalmente() {
    statusResposta = 204;
    cliente.encerrarSessoes("access-fixture");
    assertEquals("/auth/v1/logout?scope=global", caminho.get());
    assertEquals("Bearer access-fixture", autorizacao.get());
  }

  @Test
  void naoExponheCorpoDoProvedorEmErrosDeCredenciais() {
    statusResposta = 400;
    resposta = "{\"error\":\"conteudo-privado\"}";
    var erro =
        assertThrows(
            BadCredentialsException.class,
            () -> cliente.autenticar("conta@example.test", "senha-fixture"));
    assertFalse(erro.getMessage().contains("conteudo-privado"));
  }

  @Test
  void naoContaIndisponibilidadeComoSenhaIncorreta() {
    statusResposta = 503;
    assertThrows(
        IntegracaoIndisponivelException.class,
        () -> cliente.autenticar("conta@example.test", "senha-fixture"));
  }
}
