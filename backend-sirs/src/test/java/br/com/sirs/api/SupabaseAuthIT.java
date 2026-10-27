package br.com.sirs.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.sirs.api.integration.supabase.ClienteSupabase;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    properties = {
      "SUPABASE_DB_URL=${SIRS_TEST_DB_URL}",
      "SUPABASE_DB_USERNAME=${SIRS_TEST_DB_USERNAME:sirs_test}",
      "SUPABASE_DB_PASSWORD=${SIRS_TEST_DB_PASSWORD:fixture}",
      "SUPABASE_URL=https://projeto.supabase.test",
      "SUPABASE_JWKS_URL=https://projeto.supabase.test/auth/v1/.well-known/jwks.json",
      "SUPABASE_PUBLISHABLE_KEY=publishable-fixture",
      "SUPABASE_SECRET_KEY=secret-fixture",
      // O PostgreSQL descartavel local nao possui TLS. O perfil versionado exige sslmode=require.
      "spring.datasource.hikari.data-source-properties.sslmode=disable",
      "sirs.environment-file.enabled=false"
    })
@ActiveProfiles({"test", "supabase"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SIRS_TEST_DB_URL", matches = ".+")
class SupabaseAuthIT {
  @Autowired private MockMvc requisicoes;
  @Autowired private JdbcTemplate bancoDados;
  @Autowired private JsonMapper conversor;
  @Autowired private PasswordEncoder senhas;
  @MockitoBean private ClienteSupabase cliente;
  @MockitoBean private JwtDecoder decoder;
  private UUID identidade;
  private UUID sessao;
  private Long usuarioId;
  private static final String SENHA = "SenhaSupabaseFixture123!";

  @BeforeEach
  void preparar() {
    assertTrue(
        System.getenv("SIRS_TEST_DB_URL").matches("jdbc:postgresql://[^/]+/sirs_test(?:[?].*)?"));
    assertEquals("sirs", bancoDados.queryForObject("select current_schema()", String.class));
    bancoDados.execute("truncate sessao_supabase,usuario restart identity cascade");
    identidade = UUID.randomUUID();
    sessao = UUID.randomUUID();
    usuarioId = criarUsuario("recrutador", "RECRUTADOR", identidade);
    configurarSessao("recrutador", identidade, sessao, "access-recrutador", "refresh-recrutador");
  }

  private Long criarUsuario(String nome, String perfil, UUID externo) {
    return bancoDados.queryForObject(
        "insert into usuario(nome,email,senha_hash,perfil,supabase_usuario_id) values (?,?,?,?,?) returning id",
        Long.class,
        nome,
        nome + "@sirs.test",
        "hash-local-nao-utilizado",
        perfil,
        externo);
  }

  private void configurarSessao(
      String nome,
      UUID externo,
      UUID identificadorSessao,
      String accessToken,
      String refreshToken) {
    when(cliente.autenticar(nome + "@sirs.test", SENHA))
        .thenReturn(
            new ClienteSupabase.SessaoRemota(
                accessToken,
                refreshToken,
                new ClienteSupabase.UsuarioRemoto(externo, nome + "@sirs.test")));
    when(decoder.decode(accessToken)).thenReturn(token(externo, identificadorSessao, accessToken));
  }

  private Jwt token(UUID externo, UUID identificadorSessao, String valor) {
    return Jwt.withTokenValue(valor)
        .header("alg", "ES256")
        .issuer("https://projeto.supabase.test/auth/v1")
        .subject(externo.toString())
        .audience(List.of("authenticated"))
        .issuedAt(Instant.now().minusSeconds(30))
        .expiresAt(Instant.now().plusSeconds(300))
        .claim("session_id", identificadorSessao.toString())
        .claim("user_metadata", Map.of("perfil", "ADMIN"))
        .build();
  }

  private JsonNode executar(
      MockHttpServletRequestBuilder requisicao, String token, Object corpo, int esperado)
      throws Exception {
    if (token != null) requisicao.header("Authorization", "Bearer " + token);
    if (corpo != null)
      requisicao
          .contentType(MediaType.APPLICATION_JSON)
          .content(conversor.writeValueAsString(corpo));
    var resposta =
        requisicoes
            .perform(requisicao)
            .andExpect(status().is(esperado))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return resposta.isBlank() ? conversor.nullNode() : conversor.readTree(resposta);
  }

  private JsonNode login(String nome) throws Exception {
    return executar(
        post("/api/v1/auth/login"),
        null,
        Map.of("email", nome + "@sirs.test", "senha", SENHA),
        200);
  }

  private String administrador() throws Exception {
    var externo = UUID.randomUUID();
    criarUsuario("admin", "ADMIN", externo);
    configurarSessao("admin", externo, UUID.randomUUID(), "access-admin", "refresh-admin");
    return login("admin").path("accessToken").asString();
  }

  @Test
  void isolaSchemaEExecutaMigrationsDoSupabase() {
    assertEquals(
        5,
        bancoDados.queryForObject(
            "select count(*) from flyway_schema_history where success and version is not null",
            Integer.class));
    assertEquals(
        "sirs",
        bancoDados.queryForObject(
            "select table_schema from information_schema.tables where table_schema=current_schema() and table_name='sessao_supabase'",
            String.class));
  }

  @Test
  void autenticaUuidExternoMantendoPerfilLocal() throws Exception {
    var resposta = login("recrutador");
    assertEquals("access-recrutador", resposta.path("accessToken").asString());
    assertEquals("refresh-recrutador", resposta.path("refreshToken").asString());
    assertEquals(usuarioId.longValue(), resposta.path("user").path("identificador").asLong());
    var atual = executar(get("/api/v1/auth/me"), "access-recrutador", null, 200);
    assertEquals("RECRUTADOR", atual.path("perfil").asString());
    executar(get("/api/v1/usuarios"), "access-recrutador", null, 403);
  }

  @Test
  void recusaJwtSemSessaoRegistradaNoSirs() throws Exception {
    executar(get("/api/v1/auth/me"), "access-recrutador", null, 401);
    assertEquals(
        0, bancoDados.queryForObject("select count(*) from sessao_supabase", Integer.class));
  }

  @Test
  void renovaERevogaInclusiveTokensRenovados() throws Exception {
    login("recrutador");
    when(cliente.renovar("refresh-recrutador"))
        .thenReturn(
            new ClienteSupabase.SessaoRemota(
                "access-renovado",
                "refresh-renovado",
                new ClienteSupabase.UsuarioRemoto(identidade, "recrutador@sirs.test")));
    when(decoder.decode("access-renovado"))
        .thenReturn(token(identidade, sessao, "access-renovado"));
    var resposta =
        executar(
            post("/api/v1/auth/refresh"), null, Map.of("refreshToken", "refresh-recrutador"), 200);
    assertEquals("refresh-renovado", resposta.path("refreshToken").asString());
    executar(post("/api/v1/auth/logout"), "access-recrutador", null, 204);
    executar(get("/api/v1/auth/me"), "access-renovado", null, 401);
    executar(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", "refresh-recrutador"), 401);
    verify(cliente).encerrarSessoes("access-recrutador");
  }

  @Test
  void impedeSessaoVinculadaAOutroUsuario() throws Exception {
    Long outro = criarUsuario("outro", "GESTOR", UUID.randomUUID());
    bancoDados.update(
        "insert into sessao_supabase(id,usuario_id,criada_em) values (?,?,now())", sessao, outro);
    executar(get("/api/v1/auth/me"), "access-recrutador", null, 401);
  }

  @Test
  void bloqueiaTentativasInvalidasSemUsarHashLocal() throws Exception {
    when(cliente.autenticar("recrutador@sirs.test", SENHA))
        .thenThrow(new BadCredentialsException("fixture"));
    for (int tentativa = 0; tentativa < 5; tentativa++)
      executar(
          post("/api/v1/auth/login"),
          null,
          Map.of("email", "recrutador@sirs.test", "senha", SENHA),
          401);
    assertTrue(
        bancoDados.queryForObject(
            "select bloqueado from usuario where id=?", Boolean.class, usuarioId));
    assertEquals(
        5,
        bancoDados.queryForObject(
            "select tentativas_falhas from usuario where id=?", Integer.class, usuarioId));
  }

  @Test
  void criaContaSupabaseSemGuardarHashDaSenhaRemota() throws Exception {
    var admin = administrador();
    var novaIdentidade = UUID.randomUUID();
    when(cliente.criarUsuario("novo@sirs.test", SENHA))
        .thenReturn(new ClienteSupabase.UsuarioRemoto(novaIdentidade, "novo@sirs.test"));
    executar(
        post("/api/v1/usuarios"),
        admin,
        Map.of("nome", "Novo", "email", "novo@sirs.test", "senha", SENHA, "perfil", "GESTOR"),
        201);
    assertEquals(
        novaIdentidade,
        bancoDados.queryForObject(
            "select supabase_usuario_id from usuario where email='novo@sirs.test'", UUID.class));
    assertFalse(
        senhas.matches(
            SENHA,
            bancoDados.queryForObject(
                "select senha_hash from usuario where email='novo@sirs.test'", String.class)));
  }

  @Test
  void permiteVincularContaExistenteSomenteComEmailCoincidente() throws Exception {
    var admin = administrador();
    var novo = UUID.randomUUID();
    when(cliente.consultarUsuario(novo))
        .thenReturn(new ClienteSupabase.UsuarioRemoto(novo, "outro@sirs.test"));
    executar(
        put("/api/v1/usuarios/" + usuarioId + "/supabase"),
        admin,
        Map.of("usuarioSupabaseId", novo.toString()),
        422);
    when(cliente.consultarUsuario(novo))
        .thenReturn(new ClienteSupabase.UsuarioRemoto(novo, "recrutador@sirs.test"));
    executar(
        put("/api/v1/usuarios/" + usuarioId + "/supabase"),
        admin,
        Map.of("usuarioSupabaseId", novo.toString()),
        200);
    assertEquals(
        novo,
        bancoDados.queryForObject(
            "select supabase_usuario_id from usuario where id=?", UUID.class, usuarioId));
  }

  @Test
  void mudancaDePerfilRevogaSessoesExistentes() throws Exception {
    login("recrutador");
    var admin = administrador();
    executar(
        put("/api/v1/usuarios/" + usuarioId),
        admin,
        Map.of("nome", "Recrutador", "perfil", "AVALIADOR", "ativo", true, "bloqueado", false),
        200);
    executar(get("/api/v1/auth/me"), "access-recrutador", null, 401);
  }
}
