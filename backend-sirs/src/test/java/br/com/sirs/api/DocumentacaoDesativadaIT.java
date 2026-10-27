package br.com.sirs.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "SIRS_TEST_DB_URL", matches = ".+")
class DocumentacaoDesativadaIT {
  @Autowired private MockMvc requisicoes;

  @Test
  void permiteDesativarSwaggerSemAlterarAProtecaoDaApi() throws Exception {
    for (var caminho :
        new String[] {
          "/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs.yaml"
        }) requisicoes.perform(get(caminho)).andExpect(status().isNotFound());
    requisicoes.perform(get("/actuator/health")).andExpect(status().isOk());
    requisicoes.perform(get("/api/v1/vagas")).andExpect(status().isUnauthorized());
  }
}
