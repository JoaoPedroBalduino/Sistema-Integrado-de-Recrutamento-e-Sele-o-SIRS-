package br.com.sirs.api.config;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.*;

public class ValidadorAmbienteSupabase implements EnvironmentPostProcessor, Ordered {
  @Override
  public int getOrder() {
    return ConfigDataEnvironmentPostProcessor.ORDER + 2;
  }

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment ambiente, SpringApplication aplicacao) {
    if (!ambiente.acceptsProfiles(Profiles.of("supabase"))) return;
    for (var nome : List.of("SUPABASE_DB_URL", "SUPABASE_DB_USERNAME", "SUPABASE_DB_PASSWORD"))
      exigir(ambiente, nome);
    validarJdbc(ambiente.getProperty("SUPABASE_DB_URL"));
    if (!"supabase".equals(ambiente.getProperty("sirs.auth.provider"))) return;
    for (var nome : List.of("SUPABASE_URL", "SUPABASE_PUBLISHABLE_KEY", "SUPABASE_SECRET_KEY"))
      exigir(ambiente, nome);
    var projeto = validarHttps(ambiente.getProperty("SUPABASE_URL"), "SUPABASE_URL");
    String jwks =
        ambiente.getProperty(
            "SUPABASE_JWKS_URL",
            projeto.toString().replaceAll("/$", "") + "/auth/v1/.well-known/jwks.json");
    var chaves = validarHttps(jwks, "SUPABASE_JWKS_URL");
    if (!Objects.equals(projeto.getHost(), chaves.getHost())
        || !chaves.getPath().equals("/auth/v1/.well-known/jwks.json"))
      throw new IllegalStateException(
          "SUPABASE_JWKS_URL deve pertencer ao mesmo projeto e ao endpoint oficial de chaves.");
  }

  private void exigir(ConfigurableEnvironment ambiente, String nome) {
    if (ambiente.getProperty(nome, "").isBlank())
      throw new IllegalStateException(
          "Preencha " + nome + " no .env local ou no ambiente de execucao.");
  }

  private URI validarHttps(String valor, String nome) {
    try {
      var uri = URI.create(valor);
      if (!"https".equals(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getQuery() != null
          || uri.getFragment() != null) throw new IllegalArgumentException();
      return uri;
    } catch (RuntimeException excecao) {
      throw new IllegalStateException(
          nome + " deve ser uma URL HTTPS valida, sem credenciais ou parametros.");
    }
  }

  void validarJdbc(String valor) {
    try {
      if (!valor.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException();
      var uri = URI.create(valor.substring(5));
      if (uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getPort() == 6543
          || uri.getPath() == null
          || uri.getPath().length() < 2
          || uri.getFragment() != null) throw new IllegalArgumentException();
      if (uri.getQuery() != null)
        for (var parametro : uri.getQuery().split("&")) {
          var partes = parametro.split("=", 2);
          String chave =
              URLDecoder.decode(partes[0], StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
          String conteudo =
              partes.length == 2 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
          if (Set.of("user", "password").contains(chave)
              || (chave.equals("sslmode")
                  && !Set.of("require", "verify-ca", "verify-full").contains(conteudo))
              || (chave.equals("currentschema") && !conteudo.equals("sirs")))
            throw new IllegalArgumentException();
        }
    } catch (RuntimeException excecao) {
      throw new IllegalStateException(
          "SUPABASE_DB_URL deve usar JDBC PostgreSQL direto ou Session pooler, sem credenciais na URL, sem desativar SSL e sem alterar o schema sirs. Use a porta 5432, nao 6543.");
    }
  }
}
