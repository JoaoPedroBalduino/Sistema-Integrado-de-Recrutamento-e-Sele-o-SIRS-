package br.com.sirs.api.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class CarregadorAmbienteLocalTest {
  @TempDir Path diretorio;

  @Test
  void carregaArquivoComAspasSemAlterarPropriedadesGlobais() throws Exception {
    Files.writeString(
        diretorio.resolve(".env"),
        "JWT_SECRET=\"chave-apenas-teste\"\nSUPABASE_DB_PASSWORD='valor#reservado'\n");
    var ambiente = new MockEnvironment().withProperty("user.dir", diretorio.toString());
    new CarregadorAmbienteLocal().postProcessEnvironment(ambiente, new SpringApplication());
    assertEquals("chave-apenas-teste", ambiente.getProperty("JWT_SECRET"));
    assertEquals("valor#reservado", ambiente.getProperty("SUPABASE_DB_PASSWORD"));
    assertNull(System.getProperty("JWT_SECRET"));
  }

  @Test
  void preservaPrioridadeDoAmbienteDeExecucao() throws Exception {
    Files.writeString(diretorio.resolve(".env"), "JWT_SECRET=arquivo\n");
    var ambiente =
        new MockEnvironment()
            .withProperty("user.dir", diretorio.toString())
            .withProperty("JWT_SECRET", "ambiente");
    new CarregadorAmbienteLocal().postProcessEnvironment(ambiente, new SpringApplication());
    assertEquals("ambiente", ambiente.getProperty("JWT_SECRET"));
  }

  @Test
  void procuraArquivoDoModuloQuandoExecutadoNaRaiz() throws Exception {
    Files.createDirectory(diretorio.resolve("backend-sirs"));
    Files.writeString(diretorio.resolve("backend-sirs/.env"), "JWT_SECRET=modulo\n");
    var ambiente = new MockEnvironment().withProperty("user.dir", diretorio.toString());
    new CarregadorAmbienteLocal().postProcessEnvironment(ambiente, new SpringApplication());
    assertEquals("modulo", ambiente.getProperty("JWT_SECRET"));
  }

  @Test
  void permiteDesativarLeituraDuranteTestes() throws Exception {
    Files.writeString(diretorio.resolve(".env"), "JWT_SECRET=nao-carregar\n");
    var ambiente =
        new MockEnvironment()
            .withProperty("user.dir", diretorio.toString())
            .withProperty("sirs.environment-file.enabled", "false");
    new CarregadorAmbienteLocal().postProcessEnvironment(ambiente, new SpringApplication());
    assertNull(ambiente.getProperty("JWT_SECRET"));
  }

  @Test
  void arquivoAusenteNaoImpedeExecucao() {
    var ambiente = new MockEnvironment().withProperty("user.dir", diretorio.toString());
    assertDoesNotThrow(
        () ->
            new CarregadorAmbienteLocal()
                .postProcessEnvironment(ambiente, new SpringApplication()));
  }
}
