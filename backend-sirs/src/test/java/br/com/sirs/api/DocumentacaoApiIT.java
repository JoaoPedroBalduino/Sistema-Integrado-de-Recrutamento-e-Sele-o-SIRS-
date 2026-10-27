package br.com.sirs.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "SIRS_TEST_DB_URL", matches = ".+")
class DocumentacaoApiIT {
  @Autowired private MockMvc requisicoes;
  @Autowired private JsonMapper conversorJson;

  private JsonNode consultarDocumentacao() throws Exception {
    var resposta =
        requisicoes
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertNotNull(resposta.getHeader("X-Request-Id"));
    return conversorJson.readTree(resposta.getContentAsString());
  }

  @Test
  void disponibilizaInterfaceSwaggerSemAutenticacao() throws Exception {
    var redirecionamento =
        requisicoes
            .perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse();
    assertTrue(redirecionamento.getHeader("Location").contains("/swagger-ui/index.html"));
    var interfaceSwagger =
        requisicoes
            .perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(interfaceSwagger.contains("Swagger UI"));
    requisicoes.perform(get("/swagger-ui/swagger-ui.css")).andExpect(status().isOk());
    requisicoes.perform(get("/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
    var configuracao =
        conversorJson.readTree(
            requisicoes
                .perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertEquals("/v3/api-docs", configuracao.path("url").asString());
    assertFalse(configuracao.path("persistAuthorization").asBoolean());
  }

  @Test
  void documentaJwtSemExigirTokenNoLogin() throws Exception {
    var documentacao = consultarDocumentacao();
    assertEquals("SIRS API", documentacao.path("info").path("title").asString());
    var esquema = documentacao.path("components").path("securitySchemes").path("bearerAuth");
    assertEquals("http", esquema.path("type").asString());
    assertEquals("bearer", esquema.path("scheme").asString());
    assertEquals("JWT", esquema.path("bearerFormat").asString());
    assertTrue(documentacao.path("security").path(0).has("bearerAuth"));
    var login = documentacao.path("paths").path("/api/v1/auth/login").path("post");
    assertTrue(login.path("security").isArray());
    assertEquals(0, login.path("security").size());
    assertEquals(
        "#/components/schemas/RespostaAutenticacao",
        login
            .path("responses")
            .path("200")
            .path("content")
            .path("application/json")
            .path("schema")
            .path("$ref")
            .asString());
  }

  @Test
  void documentaTodasOperacoesComTagsRespostasEIdentificadoresUnicos() throws Exception {
    var documentacao = consultarDocumentacao();
    Set<String> identificadores = new HashSet<>();
    Set<String> tags = new HashSet<>();
    var caminhos = documentacao.path("paths");
    for (var caminho : caminhos.propertyNames()) {
      assertTrue(caminho.startsWith("/api/v1/"));
      for (var metodo : List.of("get", "post", "put", "patch", "delete")) {
        var operacao = caminhos.path(caminho).path(metodo);
        if (operacao.isMissingNode()) continue;
        assertFalse(operacao.path("summary").asString().isBlank());
        assertTrue(identificadores.add(operacao.path("operationId").asString()));
        assertTrue(operacao.path("tags").size() > 0);
        for (var tag : operacao.path("tags")) tags.add(tag.asString());
        assertTrue(operacao.path("responses").has("401"));
        assertTrue(operacao.path("responses").has("500"));
      }
    }
    assertEquals(61, identificadores.size());
    assertEquals(13, tags.size());
    var criacaoVaga = caminhos.path("/api/v1/vagas").path("post").path("responses");
    assertTrue(criacaoVaga.has("201"));
    assertEquals(
        "#/components/schemas/RespostaVaga",
        criacaoVaga
            .path("201")
            .path("content")
            .path("application/json")
            .path("schema")
            .path("$ref")
            .asString());
    assertEquals(
        "#/components/responses/RegraNegocio", criacaoVaga.path("422").path("$ref").asString());
    var logout = caminhos.path("/api/v1/auth/logout").path("post").path("responses").path("204");
    assertTrue(logout.has("description"));
    assertTrue(logout.path("content").isMissingNode() || logout.path("content").size() == 0);
    var respostaErro = documentacao.path("components").path("responses").path("RequisicaoInvalida");
    assertEquals(
        "#/components/schemas/DetalheErro",
        respostaErro
            .path("content")
            .path("application/problem+json")
            .path("schema")
            .path("$ref")
            .asString());
    assertEquals(
        "uuid",
        documentacao
            .path("components")
            .path("schemas")
            .path("DetalheErro")
            .path("properties")
            .path("requestId")
            .path("format")
            .asString());
  }

  @Test
  void documentaPaginacaoELimitaExposicaoDasSenhas() throws Exception {
    var documentacao = consultarDocumentacao();
    var parametros =
        documentacao.path("paths").path("/api/v1/vagas").path("get").path("parameters");
    Set<String> nomes = new HashSet<>();
    for (var parametro : parametros) {
      nomes.add(parametro.path("name").asString());
      if (parametro.path("name").asString().equals("size"))
        assertEquals(100, parametro.path("schema").path("maximum").asInt());
    }
    assertTrue(nomes.containsAll(Set.of("page", "size", "sort")));
    var esquemas = documentacao.path("components").path("schemas");
    for (var nome : List.of("RequisicaoAutenticacao", "RequisicaoCadastroUsuario")) {
      var senha = esquemas.path(nome).path("properties").path("senha");
      assertEquals("password", senha.path("format").asString());
      assertTrue(senha.path("writeOnly").asBoolean());
    }
    assertFalse(esquemas.path("RespostaUsuario").path("properties").has("senha"));
    assertFalse(esquemas.path("RespostaUsuario").path("properties").has("senhaHash"));
  }

  @Test
  void disponibilizaYamlSemLiberarOsRecursosProtegidos() throws Exception {
    var resposta =
        requisicoes
            .perform(get("/v3/api-docs.yaml"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertTrue(resposta.getContentAsString().contains("openapi:"));
    requisicoes.perform(get("/api/v1/vagas")).andExpect(status().isUnauthorized());
    requisicoes.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
  }
}
