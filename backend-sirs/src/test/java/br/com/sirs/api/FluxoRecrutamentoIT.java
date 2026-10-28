package br.com.sirs.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.sirs.api.scheduler.AgendadorRetencao;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "SIRS_TEST_DB_URL", matches = ".+")
class FluxoRecrutamentoIT {
  private static final String SENHA_TESTE = "SenhaDosTestes123!";
  private static final String HASH_TESTE = new BCryptPasswordEncoder(12).encode(SENHA_TESTE);
  @Autowired private MockMvc requisicoes;
  @Autowired private JdbcTemplate bancoDados;
  @Autowired private JsonMapper conversorJson;
  @Autowired private AgendadorRetencao retencao;
  @Autowired private Clock relogio;
  private String administrador;
  private String coordenador;
  private String recrutador;
  private String gestor;
  private String avaliador;
  private String diretoria;
  private Long areaId;
  private Long cargoId;
  private Long canalId;
  private Long coordenadorId;
  private Long recrutadorId;
  private Long gestorId;
  private Long avaliadorId;

  private record VagaPreparada(Long identificador, List<Long> etapas, List<Long> criterios) {}

  @BeforeEach
  void prepararBanco() throws Exception {
    assertTrue(
        System.getenv("SIRS_TEST_DB_URL").matches("jdbc:postgresql://[^/]+/sirs_test(?:[?].*)?"),
        "A suite integrada exige um banco chamado sirs_test.");
    // Only the explicitly configured, disposable integration database is modified.
    bancoDados.execute(
        "truncate sessao_supabase,solicitacao_titular,auditoria,visitas_canal,proposta,designacao_avaliador,avaliacao_criterio,historico_etapa,candidatura,candidato,apontamento_horas,vaga_parametro_custo,criterio_vaga,etapa_vaga,historico_status_vaga,vaga_canal,vaga,usuario,area,cargo,canal restart identity cascade");
    bancoDados.execute("truncate meta,parametro_custo restart identity");
    new ResourceDatabasePopulator(
            new ClassPathResource("db/migration/V3__parametros_referencia.sql"))
        .execute(bancoDados.getDataSource());
    cadastrarUsuarioDiretamente("administrador", "ADMIN");
    coordenadorId = cadastrarUsuarioDiretamente("coordenador", "COORD_RH");
    recrutadorId = cadastrarUsuarioDiretamente("recrutador", "RECRUTADOR");
    gestorId = cadastrarUsuarioDiretamente("gestor", "GESTOR");
    avaliadorId = cadastrarUsuarioDiretamente("avaliador", "AVALIADOR");
    cadastrarUsuarioDiretamente("diretoria", "DIRETORIA");
    administrador = autenticar("administrador");
    coordenador = autenticar("coordenador");
    recrutador = autenticar("recrutador");
    gestor = autenticar("gestor");
    avaliador = autenticar("avaliador");
    diretoria = autenticar("diretoria");
    areaId = cadastrarApoio("areas", "Tecnologia");
    cargoId = cadastrarApoio("cargos", "Desenvolvedor Backend");
    canalId = cadastrarApoio("canais", "Portal de carreiras");
  }

  private Long cadastrarUsuarioDiretamente(String nome, String perfil) {
    return bancoDados.queryForObject(
        "insert into usuario(nome,email,senha_hash,perfil) values (?,?,?,?) returning id",
        Long.class,
        nome,
        nome + "@sirs.test",
        HASH_TESTE,
        perfil);
  }

  private String autenticar(String nome) throws Exception {
    return executar(
            post("/api/v1/auth/login"),
            null,
            Map.of("email", nome + "@sirs.test", "senha", SENHA_TESTE),
            200)
        .path("accessToken")
        .asString();
  }

  private JsonNode executar(
      MockHttpServletRequestBuilder requisicao, String token, Object corpo, int statusEsperado)
      throws Exception {
    if (token != null) requisicao.header("Authorization", "Bearer " + token);
    if (corpo != null)
      requisicao
          .contentType(MediaType.APPLICATION_JSON)
          .content(conversorJson.writeValueAsString(corpo));
    var resposta =
        requisicoes
            .perform(requisicao)
            .andExpect(status().is(statusEsperado))
            .andReturn()
            .getResponse();
    assertNotNull(resposta.getHeader("X-Request-Id"));
    return resposta.getContentAsString().isBlank()
        ? conversorJson.nullNode()
        : conversorJson.readTree(resposta.getContentAsString());
  }

  private Long cadastrarApoio(String tipo, String nome) throws Exception {
    return executar(
            post("/api/v1/cadastros/" + tipo),
            administrador,
            Map.of("nome", nome, "ativo", true),
            201)
        .path("identificador")
        .asLong();
  }

  private Map<String, Object> dadosVaga(int posicoes) {
    Map<String, Object> dados = new HashMap<>();
    dados.put("titulo", "Desenvolvedor Backend");
    dados.put("cargoId", cargoId);
    dados.put("areaId", areaId);
    dados.put("solicitanteId", gestorId);
    dados.put("recrutadorId", recrutadorId);
    dados.put("posicoes", posicoes);
    dados.put("remoto", true);
    dados.put("prazoAlvoDias", 30);
    dados.put("investimentoDivulgacao", 10);
    dados.put("canalIds", List.of(canalId));
    return dados;
  }

  private VagaPreparada prepararVaga(int posicoes, boolean publicar) throws Exception {
    Long identificador =
        executar(post("/api/v1/vagas"), coordenador, dadosVaga(posicoes), 201)
            .path("identificador")
            .asLong();
    List<Long> etapas = new ArrayList<>();
    List<Long> criterios = new ArrayList<>();
    for (int ordem = 1; ordem <= 4; ordem++)
      etapas.add(
          executar(
                  post("/api/v1/vagas/" + identificador + "/etapas"),
                  coordenador,
                  Map.of("nome", "Etapa " + ordem, "ordem", ordem),
                  201)
              .path("identificador")
              .asLong());
    int[] pesos = {4, 5, 3, 4, 3};
    for (int indice = 0; indice < pesos.length; indice++)
      criterios.add(
          executar(
                  post("/api/v1/vagas/" + identificador + "/criterios"),
                  coordenador,
                  Map.of("nome", "Criterio " + indice, "peso", pesos[indice]),
                  201)
              .path("identificador")
              .asLong());
    if (publicar) {
      alterarStatusVaga(identificador, "EM_APROVACAO", coordenador, 200);
      alterarStatusVaga(identificador, "PUBLICADA", diretoria, 200);
    }
    return new VagaPreparada(identificador, etapas, criterios);
  }

  private void alterarStatusVaga(Long vagaId, String status, String token, int statusEsperado)
      throws Exception {
    executar(
        patch("/api/v1/vagas/" + vagaId + "/status"),
        token,
        Map.of("status", status),
        statusEsperado);
  }

  private Long cadastrarCandidato(String nome) throws Exception {
    return executar(
            post("/api/v1/candidatos"),
            coordenador,
            Map.of(
                "nome",
                nome,
                "email",
                nome + "@candidatos.test",
                "baseLegal",
                "CONSENTIMENTO",
                "dataColeta",
                Instant.now(relogio).minusSeconds(30).toString(),
                "versaoConsentimento",
                "1.2",
                "curriculoUrl",
                "https://curriculos.test/arquivo.pdf"),
            201)
        .path("identificador")
        .asLong();
  }

  private Long inscrever(VagaPreparada vaga, String nome) throws Exception {
    Long candidatoId = cadastrarCandidato(nome);
    return executar(
            post("/api/v1/candidaturas"),
            coordenador,
            Map.of("vagaId", vaga.identificador(), "candidatoId", candidatoId, "canalId", canalId),
            201)
        .path("identificador")
        .asLong();
  }

  private void aprovarEOferecer(VagaPreparada vaga, Long candidaturaId) throws Exception {
    executar(
        patch("/api/v1/candidaturas/" + candidaturaId + "/etapa"),
        coordenador,
        Map.of("etapaId", vaga.etapas().get(3)),
        200);
    executar(
        patch("/api/v1/candidaturas/" + candidaturaId + "/status"),
        coordenador,
        Map.of("status", "APROVADO"),
        200);
    executar(
        post("/api/v1/candidaturas/" + candidaturaId + "/proposta"),
        coordenador,
        Map.of("salarioOfertado", 4000, "dataProposta", LocalDate.now(relogio).toString()),
        201);
  }

  @Test
  void sobeComMigrationsEProtegeRotas() throws Exception {
    executar(get("/actuator/health"), null, null, 200);
    executar(get("/api/v1/vagas"), null, null, 401);
    executar(
        get("/api/v1/vagas").header("Authorization", "Bearer token-invalido"), null, null, 401);
    executar(get("/v3/api-docs"), null, null, 200);
    executar(get("/api/v1/rota-inexistente"), coordenador, null, 404);
    executar(get("/api/v1/vagas?sort=senhaHash,asc"), coordenador, null, 400);
    assertEquals(
        4,
        bancoDados.queryForObject(
            "select count(*) from flyway_schema_history where success", Integer.class));
  }

  @Test
  void executaFluxoCompletoComNotaCustosEContratacao() throws Exception {
    var vaga = prepararVaga(1, true);
    Long candidaturaId = inscrever(vaga, "roberto");
    int[] notas = {4, 2, 4, 3, 5};
    List<Map<String, Object>> avaliacoes = new ArrayList<>();
    for (int indice = 0; indice < notas.length; indice++)
      avaliacoes.add(Map.of("criterioId", vaga.criterios().get(indice), "nota", notas[indice]));
    var resultado =
        executar(
            put("/api/v1/candidaturas/" + candidaturaId + "/avaliacoes"),
            coordenador,
            Map.of("notas", avaliacoes),
            200);
    assertEquals("0.6842", resultado.path("notaFinal").asString());
    executar(
        post("/api/v1/vagas/" + vaga.identificador() + "/horas"),
        recrutador,
        Map.of("data", LocalDate.now(relogio).toString(), "horas", 8.5, "atividade", "Entrevistas"),
        201);
    var custo =
        executar(get("/api/v1/vagas/" + vaga.identificador() + "/custos"), coordenador, null, 200);
    assertEquals(573.86, custo.path("total").asDouble(), 0.001);
    aprovarEOferecer(vaga, candidaturaId);
    executar(
        post("/api/v1/candidaturas/" + candidaturaId + "/contratacao"),
        coordenador,
        Map.of("dataAceite", LocalDate.now(relogio).toString()),
        200);
    executar(
        post("/api/v1/candidaturas/" + candidaturaId + "/contratacao"),
        coordenador,
        Map.of("dataAceite", LocalDate.now(relogio).toString()),
        422);
    var detalhe = executar(get("/api/v1/vagas/" + vaga.identificador()), coordenador, null, 200);
    assertEquals(1, detalhe.path("vaga").path("posicoesPreenchidas").asInt());
    assertEquals(1, detalhe.path("vaga").path("candidatos").asInt());
    alterarStatusVaga(vaga.identificador(), "FECHADA", coordenador, 200);
    alterarStatusVaga(vaga.identificador(), "PUBLICADA", coordenador, 422);
    var resumo =
        executar(
            get(
                "/api/v1/indicadores/resumo?de="
                    + LocalDate.now(relogio).withDayOfMonth(1)
                    + "&ate="
                    + LocalDate.now(relogio)),
            coordenador,
            null,
            200);
    assertEquals(8.5, resumo.path("horasPorVagaFechada").asDouble(), 0.001);
    assertEquals(573.86, resumo.path("custoPorVagaFechada").asDouble(), 0.001);
  }

  @Test
  void respeitaMatrizDePerfisEItensDesignados() throws Exception {
    var vaga = prepararVaga(1, true);
    Long candidaturaId = inscrever(vaga, "designado");
    executar(get("/api/v1/candidaturas/" + candidaturaId), avaliador, null, 403);
    executar(
        post("/api/v1/candidaturas/" + candidaturaId + "/avaliadores"),
        coordenador,
        Map.of("usuarioId", avaliadorId),
        204);
    var fichaAvaliador =
        executar(get("/api/v1/candidaturas/" + candidaturaId), avaliador, null, 200);
    assertEquals(vaga.etapas().size(), fichaAvaliador.path("etapas").size());
    assertEquals(vaga.criterios().size(), fichaAvaliador.path("criterios").size());
    assertTrue(fichaAvaliador.path("proposta").isNull());
    assertFalse(fichaAvaliador.has("apontamentos"));
    executar(
        put("/api/v1/candidaturas/" + candidaturaId + "/avaliacoes"),
        avaliador,
        Map.of("notas", List.of(Map.of("criterioId", vaga.criterios().get(0), "nota", 5))),
        200);
    executar(
        patch("/api/v1/candidaturas/" + candidaturaId + "/status"),
        avaliador,
        Map.of("status", "APROVADO"),
        403);
    executar(post("/api/v1/vagas"), administrador, dadosVaga(1), 403);
    executar(get("/api/v1/candidaturas"), diretoria, null, 403);
    Long outroGestorId = cadastrarUsuarioDiretamente("outrogestor", "GESTOR");
    String outroGestor = autenticar("outrogestor");
    executar(get("/api/v1/vagas/" + vaga.identificador()), outroGestor, null, 403);
    var lista = executar(get("/api/v1/vagas"), outroGestor, null, 200);
    assertEquals(0, lista.path("totalElements").asInt());
    executar(get("/api/v1/vagas/" + vaga.identificador()), gestor, null, 200);
    assertTrue(outroGestorId > 0);
  }

  @Test
  void bloqueiaContaERevogaTokensImediatamente() throws Exception {
    for (int tentativa = 0; tentativa < 5; tentativa++)
      executar(
          post("/api/v1/auth/login"),
          null,
          Map.of("email", "recrutador@sirs.test", "senha", "senha-incorreta"),
          401);
    assertTrue(
        bancoDados.queryForObject(
            "select bloqueado from usuario where id=?", Boolean.class, recrutadorId));
    assertEquals(
        5,
        bancoDados.queryForObject(
            "select tentativas_falhas from usuario where id=?", Integer.class, recrutadorId));
    executar(get("/api/v1/auth/me"), recrutador, null, 401);
    executar(
        put("/api/v1/usuarios/" + recrutadorId),
        administrador,
        Map.of("nome", "Recrutador", "perfil", "RECRUTADOR", "ativo", true, "bloqueado", false),
        200);
    String novoToken = autenticar("recrutador");
    executar(get("/api/v1/auth/me"), novoToken, null, 200);
    executar(post("/api/v1/auth/logout"), novoToken, null, 204);
    executar(get("/api/v1/auth/me"), novoToken, null, 401);
  }

  @Test
  void validaEntradaERejeitaReferenciasDeOutraVaga() throws Exception {
    executar(post("/api/v1/vagas"), coordenador, dadosVaga(0), 400);
    var vaga = prepararVaga(1, true);
    Long candidaturaId = inscrever(vaga, "validacao");
    var outraVaga = prepararVaga(1, true);
    executar(
        patch("/api/v1/candidaturas/" + candidaturaId + "/etapa"),
        coordenador,
        Map.of("etapaId", outraVaga.etapas().get(0)),
        422);
    executar(
        put("/api/v1/candidaturas/" + candidaturaId + "/avaliacoes"),
        coordenador,
        Map.of("notas", List.of(Map.of("criterioId", outraVaga.criterios().get(0), "nota", 3))),
        422);
    executar(
        put("/api/v1/candidaturas/" + candidaturaId + "/avaliacoes"),
        coordenador,
        Map.of("notas", List.of(Map.of("criterioId", vaga.criterios().get(0), "nota", 6))),
        400);
    Long candidatoId =
        bancoDados.queryForObject(
            "select candidato_id from candidatura where id=?", Long.class, candidaturaId);
    executar(
        post("/api/v1/candidaturas"),
        coordenador,
        Map.of("vagaId", vaga.identificador(), "candidatoId", candidatoId, "canalId", canalId),
        422);
    executar(
        post("/api/v1/candidaturas/" + candidaturaId + "/contratacao"),
        coordenador,
        Map.of("dataAceite", LocalDate.now(relogio).toString()),
        422);
    alterarStatusVaga(vaga.identificador(), "CONGELADA", coordenador, 200);
    executar(
        post("/api/v1/candidaturas"),
        coordenador,
        Map.of(
            "vagaId",
            vaga.identificador(),
            "candidatoId",
            cadastrarCandidato("congelada"),
            "canalId",
            canalId),
        422);
  }

  @Test
  void calculaFunilERelatoriosSemDivisaoPorZero() throws Exception {
    var vaga = prepararVaga(3, true);
    int[] distribuicao = {2, 2, 3, 2};
    int sequencia = 0;
    for (int indice = 0; indice < distribuicao.length; indice++) {
      for (int quantidade = 0; quantidade < distribuicao[indice]; quantidade++) {
        Long candidaturaId = inscrever(vaga, "funil" + (sequencia++));
        if (indice > 0)
          executar(
              patch("/api/v1/candidaturas/" + candidaturaId + "/etapa"),
              coordenador,
              Map.of("etapaId", vaga.etapas().get(indice)),
              200);
      }
    }
    var resultado =
        executar(get("/api/v1/indicadores/vagas/" + vaga.identificador()), gestor, null, 200);
    var funil = resultado.path("funil");
    assertEquals(9, funil.path(0).path("passaram").asInt());
    assertEquals(7, funil.path(1).path("passaram").asInt());
    assertEquals(5, funil.path(2).path("passaram").asInt());
    assertEquals(2, funil.path(3).path("passaram").asInt());
    assertEquals(77.78, funil.path(0).path("percentualConversao").asDouble(), 0.001);
    String periodo =
        "?de=" + LocalDate.now(relogio).withDayOfMonth(1) + "&ate=" + LocalDate.now(relogio);
    executar(get("/api/v1/indicadores/cargos" + periodo), coordenador, null, 200);
    executar(get("/api/v1/indicadores/recrutadores" + periodo), gestor, null, 200);
    var canais = executar(get("/api/v1/indicadores/canais" + periodo), coordenador, null, 200);
    assertEquals(0, canais.path(0).path("percentualCanal").asDouble());
    executar(
        put("/api/v1/visitas-canais"),
        recrutador,
        Map.of(
            "canalId",
            canalId,
            "periodo",
            LocalDate.now(relogio).withDayOfMonth(1).toString(),
            "visitas",
            150),
        200);
    executar(
        put("/api/v1/visitas-canais"),
        recrutador,
        Map.of(
            "canalId",
            canalId,
            "periodo",
            LocalDate.now(relogio).withDayOfMonth(2).toString(),
            "visitas",
            150),
        422);
    executar(get("/api/v1/visitas-canais"), coordenador, null, 200);
    executar(get("/api/v1/indicadores/historico" + periodo), diretoria, null, 200);
    executar(
        get("/api/v1/indicadores/resumo?de=2026-10-26&ate=2026-01-01"), coordenador, null, 422);
  }

  @Test
  void impedeContratacoesConcorrentesAcimaDaCapacidade() throws Exception {
    var vaga = prepararVaga(1, true);
    Long primeira = inscrever(vaga, "primeira");
    Long segunda = inscrever(vaga, "segunda");
    aprovarEOferecer(vaga, primeira);
    aprovarEOferecer(vaga, segunda);
    var executor = Executors.newFixedThreadPool(2);
    var inicio = new CountDownLatch(1);
    try {
      List<Future<Integer>> resultados = new ArrayList<>();
      for (Long identificador : List.of(primeira, segunda))
        resultados.add(
            executor.submit(
                () -> {
                  inicio.await();
                  return requisicoes
                      .perform(
                          post("/api/v1/candidaturas/" + identificador + "/contratacao")
                              .header("Authorization", "Bearer " + coordenador)
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(
                                  conversorJson.writeValueAsString(
                                      Map.of("dataAceite", LocalDate.now(relogio).toString()))))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      inicio.countDown();
      List<Integer> codigos = new ArrayList<>();
      for (var resultado : resultados) codigos.add(resultado.get(20, TimeUnit.SECONDS));
      Collections.sort(codigos);
      assertEquals(List.of(200, 422), codigos);
      assertEquals(
          1,
          bancoDados.queryForObject(
              "select count(*) from candidatura where vaga_id=? and contratada_em is not null",
              Integer.class,
              vaga.identificador()));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void registraSolicitacaoExportaEAnonimizaSemCopiarDadosParaAuditoria() throws Exception {
    Long candidatoId = cadastrarCandidato("privacidade");
    var solicitacao =
        executar(
            post("/api/v1/privacidade/solicitacoes"),
            administrador,
            Map.of("candidatoId", candidatoId, "tipo", "ANONIMIZACAO"),
            201);
    var dataSolicitacao =
        Instant.parse(solicitacao.path("solicitadaEm").asString())
            .atZone(relogio.getZone())
            .toLocalDate();
    assertEquals(dataSolicitacao.plusDays(15).toString(), solicitacao.path("prazo").asString());
    executar(
        get("/api/v1/privacidade/candidatos/" + candidatoId + "/exportacao"),
        administrador,
        null,
        200);
    executar(
        post(
            "/api/v1/privacidade/solicitacoes/"
                + solicitacao.path("identificador").asLong()
                + "/conclusao"),
        administrador,
        null,
        200);
    var candidato = executar(get("/api/v1/candidatos/" + candidatoId), coordenador, null, 200);
    assertTrue(candidato.path("curriculoUrl").isNull());
    assertTrue(candidato.path("anonimizadoEm").isString());
    assertEquals(
        "anonimizado-" + candidatoId + "@invalid.local", candidato.path("email").asString());
    executar(get("/api/v1/privacidade/solicitacoes"), coordenador, null, 200);
    executar(get("/api/v1/auditoria"), administrador, null, 200);
    assertEquals(
        0,
        bancoDados.queryForObject(
            "select count(*) from auditoria where dados_antes is not null or dados_depois is not null",
            Integer.class));
    executar(
        post("/api/v1/privacidade/solicitacoes"),
        recrutador,
        Map.of("candidatoId", candidatoId, "tipo", "ACESSO"),
        403);
  }

  @Test
  void validaVigenciasEAjustesDeCustos() throws Exception {
    executar(
        post("/api/v1/configuracoes/parametros-custo"),
        coordenador,
        Map.of("chave", "HORAS_MENSAIS", "valor", 0, "vigenciaInicio", "2030-01-01"),
        422);
    executar(
        post("/api/v1/configuracoes/parametros-custo"),
        coordenador,
        Map.of("chave", "HORAS_MENSAIS", "valor", 220, "vigenciaInicio", "2030-01-01"),
        422);
    executar(
        post("/api/v1/configuracoes/metas"),
        coordenador,
        Map.of("chave", "VAGAS_NO_PRAZO", "valor", 101, "vigenciaInicio", "2030-01-01"),
        422);
    executar(get("/api/v1/configuracoes/parametros-custo"), administrador, null, 200);
    executar(get("/api/v1/configuracoes/metas"), diretoria, null, 200);
    var vaga = prepararVaga(1, true);
    executar(
        put("/api/v1/vagas/" + vaga.identificador() + "/custos/parametros"),
        coordenador,
        Map.of("parametros", Map.of("CURRICULOS_TRIADOS", 100)),
        204);
    var resultado =
        executar(get("/api/v1/vagas/" + vaga.identificador() + "/custos"), coordenador, null, 200);
    assertTrue(resultado.path("total").asDouble() < 573.86);
    executar(
        put("/api/v1/vagas/" + vaga.identificador() + "/custos/parametros"),
        recrutador,
        Map.of("parametros", Map.of("CURRICULOS_TRIADOS", 100)),
        403);
  }

  @Test
  void preservaMetasDeCadaMesSemSobreporVigencias() throws Exception {
    Long metaId =
        bancoDados.queryForObject(
            "select id from meta where indicador='TEMPO_FECHAMENTO'", Long.class);
    executar(
        patch("/api/v1/configuracoes/metas/" + metaId + "/vigencia"),
        coordenador,
        Map.of("vigenciaFim", "2026-08-31"),
        204);
    executar(
        post("/api/v1/configuracoes/metas"),
        coordenador,
        Map.of("chave", "TEMPO_FECHAMENTO", "valor", 5, "vigenciaInicio", "2026-09-01"),
        201);
    var historico =
        executar(
            get("/api/v1/indicadores/historico?de=2026-08-01&ate=2026-09-30"),
            coordenador,
            null,
            200);
    assertEquals(4, historico.path(0).path("metasVigentes").path("TEMPO_FECHAMENTO").asInt());
    assertEquals(5, historico.path(1).path("metasVigentes").path("TEMPO_FECHAMENTO").asInt());
  }

  @Test
  void permiteEditarEstruturaApenasEmRascunho() throws Exception {
    var vaga = prepararVaga(1, false);
    executar(
        put("/api/v1/vagas/" + vaga.identificador() + "/etapas/" + vaga.etapas().get(0)),
        coordenador,
        Map.of("nome", "Triagem inicial", "ordem", 1),
        200);
    executar(
        put("/api/v1/vagas/" + vaga.identificador() + "/criterios/" + vaga.criterios().get(0)),
        coordenador,
        Map.of("nome", "Tecnica", "peso", 5),
        200);
    executar(get("/api/v1/cadastros/recrutadores"), recrutador, null, 200);
    executar(get("/api/v1/cadastros/solicitantes"), gestor, null, 200);
    alterarStatusVaga(vaga.identificador(), "EM_APROVACAO", coordenador, 200);
    executar(
        put("/api/v1/vagas/" + vaga.identificador() + "/etapas/" + vaga.etapas().get(0)),
        coordenador,
        Map.of("nome", "Outra", "ordem", 1),
        422);
    alterarStatusVaga(vaga.identificador(), "PUBLICADA", recrutador, 403);
  }

  @Test
  void anonimizaRetencaoElegivelEPreservaCandidatosAtivos() throws Exception {
    Long antigo = cadastrarCandidato("antigo");
    Long recente = cadastrarCandidato("recente");
    var vaga = prepararVaga(1, true);
    Long inscricao = inscrever(vaga, "ativoantigo");
    Long ativo =
        bancoDados.queryForObject(
            "select candidato_id from candidatura where id=?", Long.class, inscricao);
    bancoDados.update(
        "update candidato set ultima_atividade_em=now()-interval '13 months' where id in (?,?)",
        antigo,
        ativo);
    retencao.executarRetencao();
    assertNotNull(
        bancoDados.queryForObject(
            "select anonimizado_em from candidato where id=?", Timestamp.class, antigo));
    assertNull(
        bancoDados.queryForObject(
            "select anonimizado_em from candidato where id=?", Timestamp.class, recente));
    assertNull(
        bancoDados.queryForObject(
            "select anonimizado_em from candidato where id=?", Timestamp.class, ativo));
  }

  @Test
  void colocaNotasAusentesNoFimDoRanking() throws Exception {
    var vaga = prepararVaga(1, true);
    inscrever(vaga, "semnota");
    Long avaliada = inscrever(vaga, "comnota");
    executar(
        put("/api/v1/candidaturas/" + avaliada + "/avaliacoes"),
        coordenador,
        Map.of("notas", List.of(Map.of("criterioId", vaga.criterios().get(0), "nota", 4))),
        200);
    var ranking =
        executar(
            get("/api/v1/candidaturas?vagaId=" + vaga.identificador()), coordenador, null, 200);
    assertEquals(
        avaliada.longValue(), ranking.path("content").path(0).path("identificador").asLong());
    assertTrue(ranking.path("content").path(1).path("notaFinal").isNull());
  }
}
