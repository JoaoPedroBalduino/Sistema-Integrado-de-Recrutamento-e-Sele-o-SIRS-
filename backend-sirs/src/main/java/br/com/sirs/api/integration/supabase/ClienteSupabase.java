package br.com.sirs.api.integration.supabase;

import br.com.sirs.api.config.PropriedadesSupabase;
import br.com.sirs.api.exception.*;
import com.fasterxml.jackson.annotation.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
@ConditionalOnProperty(name = "sirs.auth.provider", havingValue = "supabase")
public class ClienteSupabase {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record UsuarioRemoto(UUID id, String email) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SessaoRemota(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("refresh_token") String refreshToken,
      UsuarioRemoto user) {}

  private final RestClient publico;
  private final RestClient administrativo;

  public ClienteSupabase(PropriedadesSupabase propriedades, RestClient.Builder construtor) {
    var transporte =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    transporte.setReadTimeout(Duration.ofSeconds(10));
    var base =
        construtor
            .clone()
            .baseUrl(propriedades.getUrl().toString().replaceAll("/$", ""))
            .requestFactory(transporte)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    publico = base.clone().defaultHeader("apikey", propriedades.getPublishableKey()).build();
    administrativo = base.clone().defaultHeader("apikey", propriedades.getSecretKey()).build();
  }

  public SessaoRemota autenticar(String email, String senha) {
    return trocarToken("password", Map.of("email", email, "password", senha));
  }

  public SessaoRemota renovar(String refreshToken) {
    return trocarToken("refresh_token", Map.of("refresh_token", refreshToken));
  }

  private SessaoRemota trocarToken(String concessao, Map<String, String> corpo) {
    try {
      var sessao =
          publico
              .post()
              .uri("/auth/v1/token?grant_type=" + concessao)
              .body(corpo)
              .retrieve()
              .body(SessaoRemota.class);
      if (sessao == null
          || sessao.user() == null
          || sessao.accessToken() == null
          || sessao.refreshToken() == null) throw new IntegracaoIndisponivelException();
      return sessao;
    } catch (HttpClientErrorException excecao) {
      if (excecao.getStatusCode().value() == 429) throw new IntegracaoIndisponivelException();
      throw new BadCredentialsException("Credenciais invalidas ou sessao indisponivel.");
    } catch (RestClientException excecao) {
      throw new IntegracaoIndisponivelException();
    }
  }

  public UsuarioRemoto criarUsuario(String email, String senha) {
    try {
      var usuario =
          administrativo
              .post()
              .uri("/auth/v1/admin/users")
              .body(Map.of("email", email, "password", senha, "email_confirm", true))
              .retrieve()
              .body(UsuarioRemoto.class);
      if (usuario == null || usuario.id() == null) throw new IntegracaoIndisponivelException();
      return usuario;
    } catch (HttpClientErrorException excecao) {
      if (excecao.getStatusCode().value() == 429) throw new IntegracaoIndisponivelException();
      throw new RegraNegocioException(
          "Conta existente ou senha rejeitada pelo Supabase. Use as credenciais atuais para vincular uma conta existente.");
    } catch (RestClientException excecao) {
      throw new IntegracaoIndisponivelException();
    }
  }

  public UsuarioRemoto consultarUsuario(UUID identificador) {
    try {
      var usuario =
          administrativo
              .get()
              .uri("/auth/v1/admin/users/{identificador}", identificador)
              .retrieve()
              .body(UsuarioRemoto.class);
      if (usuario == null || usuario.id() == null) throw new IntegracaoIndisponivelException();
      return usuario;
    } catch (HttpClientErrorException excecao) {
      if (excecao.getStatusCode().value() == 404)
        throw new RecursoNaoEncontradoException("Usuario Supabase", identificador);
      throw new IntegracaoIndisponivelException();
    } catch (RestClientException excecao) {
      throw new IntegracaoIndisponivelException();
    }
  }

  public void alterarSenha(String accessToken, String novaSenha) {
    try {
      publico
          .put()
          .uri("/auth/v1/user")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .body(Map.of("password", novaSenha))
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException excecao) {
      throw new RegraNegocioException("O Supabase rejeitou a alteracao de senha.");
    } catch (RestClientException excecao) {
      throw new IntegracaoIndisponivelException();
    }
  }

  public void encerrarSessoes(String accessToken) {
    try {
      publico
          .post()
          .uri("/auth/v1/logout?scope=global")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException excecao) {
      throw new IntegracaoIndisponivelException();
    }
  }
}
