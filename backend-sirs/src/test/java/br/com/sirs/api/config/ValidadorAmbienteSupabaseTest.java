package br.com.sirs.api.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class ValidadorAmbienteSupabaseTest {
  private MockEnvironment ambienteValido() {
    var ambiente =
        new MockEnvironment()
            .withProperty("SUPABASE_DB_URL", "jdbc:postgresql://pooler.supabase.test:5432/postgres")
            .withProperty("SUPABASE_DB_USERNAME", "postgres.projeto")
            .withProperty("SUPABASE_DB_PASSWORD", "senha-apenas-teste")
            .withProperty("sirs.auth.provider", "supabase")
            .withProperty("SUPABASE_URL", "https://projeto.supabase.test")
            .withProperty("SUPABASE_PUBLISHABLE_KEY", "publishable-teste")
            .withProperty("SUPABASE_SECRET_KEY", "secret-teste");
    ambiente.setActiveProfiles("supabase");
    return ambiente;
  }

  @Test
  void aceitaPoolerDeSessaoComCredenciaisSeparadas() {
    assertDoesNotThrow(
        () ->
            new ValidadorAmbienteSupabase()
                .postProcessEnvironment(ambienteValido(), new SpringApplication()));
  }

  @Test
  void rejeitaPoolerTransacional() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new ValidadorAmbienteSupabase()
                .validarJdbc("jdbc:postgresql://pooler.supabase.test:6543/postgres"));
  }

  @Test
  void rejeitaSenhaDentroDaUrlSemExporOValor() {
    var erro =
        assertThrows(
            IllegalStateException.class,
            () ->
                new ValidadorAmbienteSupabase()
                    .validarJdbc(
                        "jdbc:postgresql://pooler.supabase.test:5432/postgres?password=valor-confidencial"));
    assertFalse(erro.getMessage().contains("valor-confidencial"));
  }

  @Test
  void rejeitaDesativacaoDeSslPelaUrl() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new ValidadorAmbienteSupabase()
                .validarJdbc("jdbc:postgresql://host:5432/postgres?sslmode=disable"));
  }

  @Test
  void rejeitaSchemaPublicoNaUrl() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new ValidadorAmbienteSupabase()
                .validarJdbc("jdbc:postgresql://host:5432/postgres?currentSchema=public"));
  }

  @Test
  void exigeConfiguracaoDoBancoAntesDaConexao() {
    var ambiente = ambienteValido().withProperty("SUPABASE_DB_PASSWORD", "");
    var erro =
        assertThrows(
            IllegalStateException.class,
            () ->
                new ValidadorAmbienteSupabase()
                    .postProcessEnvironment(ambiente, new SpringApplication()));
    assertTrue(erro.getMessage().contains("SUPABASE_DB_PASSWORD"));
  }

  @Test
  void rejeitaJwksDeOutroProjeto() {
    var ambiente =
        ambienteValido()
            .withProperty(
                "SUPABASE_JWKS_URL", "https://outro.supabase.test/auth/v1/.well-known/jwks.json");
    assertThrows(
        IllegalStateException.class,
        () ->
            new ValidadorAmbienteSupabase()
                .postProcessEnvironment(ambiente, new SpringApplication()));
  }

  @Test
  void rejeitaApiSemHttps() {
    var ambiente = ambienteValido().withProperty("SUPABASE_URL", "http://projeto.supabase.test");
    assertThrows(
        IllegalStateException.class,
        () ->
            new ValidadorAmbienteSupabase()
                .postProcessEnvironment(ambiente, new SpringApplication()));
  }

  @Test
  void perfilLocalNaoExigeSupabase() {
    assertDoesNotThrow(
        () ->
            new ValidadorAmbienteSupabase()
                .postProcessEnvironment(new MockEnvironment(), new SpringApplication()));
  }
}
