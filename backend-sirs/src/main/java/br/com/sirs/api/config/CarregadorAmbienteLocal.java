package br.com.sirs.api.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.*;
import java.util.*;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.*;

/** Carrega somente arquivos locais reais, sem publicar segredos em propriedades globais da JVM. */
public class CarregadorAmbienteLocal implements EnvironmentPostProcessor, Ordered {
  @Override
  public int getOrder() {
    return ConfigDataEnvironmentPostProcessor.ORDER + 1;
  }

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment ambiente, SpringApplication aplicacao) {
    if (!ambiente.getProperty("sirs.environment-file.enabled", Boolean.class, true)) return;
    var diretorio =
        Path.of(ambiente.getProperty("user.dir", System.getProperty("user.dir")))
            .toAbsolutePath()
            .normalize();
    for (var arquivo : List.of(diretorio.resolve(".env"), diretorio.resolve("backend-sirs/.env"))) {
      if (!Files.isRegularFile(arquivo)) continue;
      try {
        var dotenv =
            Dotenv.configure()
                .directory(arquivo.getParent().toString())
                .filename(arquivo.getFileName().toString())
                .load();
        Map<String, Object> valores = new HashMap<>();
        dotenv
            .entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)
            .forEach(entrada -> valores.put(entrada.getKey(), normalizarValor(entrada.getValue())));
        ambiente.getPropertySources().addLast(new MapPropertySource("sirsLocalDotenv", valores));
        return;
      } catch (RuntimeException excecao) {
        throw new IllegalStateException(
            "Arquivo .env invalido. Confira sua sintaxe sem publicar os valores.");
      }
    }
  }

  private String normalizarValor(String valor) {
    return valor.length() >= 2
            && valor.charAt(0) == '\''
            && valor.charAt(valor.length() - 1) == '\''
        ? valor.substring(1, valor.length() - 1)
        : valor;
  }
}
