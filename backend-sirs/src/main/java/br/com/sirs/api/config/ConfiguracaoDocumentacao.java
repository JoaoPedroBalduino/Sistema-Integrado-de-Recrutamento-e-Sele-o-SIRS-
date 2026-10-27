package br.com.sirs.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info =
        @Info(
            title = "SIRS API",
            version = "v1",
            description =
                """
        Sistema Integrado de Recrutamento e SeleÃ§Ã£o.

        Para testar operaÃ§Ãµes protegidas, execute POST /api/v1/auth/login e copie o accessToken.
        Clique em Authorize e informe somente o token, sem o prefixo Bearer.
        As permissÃµes sÃ£o verificadas por perfil, vaga responsÃ¡vel e designaÃ§Ã£o de avaliador.
        No modo local, tokens expiram em 30 minutos; no Supabase, consulte expiresAt e use POST /api/v1/auth/refresh.
        Logout, troca de senha e alteracoes de acesso revogam as sessoes registradas no SIRS.
        """),
    security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token emitido pelo login. Informe somente o accessToken no botÃ£o Authorize.")
@Configuration
public class ConfiguracaoDocumentacao {
  @Bean
  OpenApiCustomizer documentarContratosComuns() {
    return documentacao -> {
      if (documentacao.getComponents() == null) documentacao.setComponents(new Components());
      var componentes = documentacao.getComponents();
      componentes.addSchemas(
          "DetalheErro",
          new ObjectSchema()
              .description(
                  "Erro HTTP padronizado. O requestId tambÃ©m Ã© enviado no cabeÃ§alho X-Request-Id.")
              .addProperty("type", new StringSchema().format("uri").example("about:blank"))
              .addProperty("title", new StringSchema().example("Unprocessable Entity"))
              .addProperty(
                  "status",
                  new IntegerSchema().minimum(new BigDecimal("400")).maximum(new BigDecimal("599")))
              .addProperty("detail", new StringSchema().example("TransiÃ§Ã£o de vaga invÃ¡lida."))
              .addProperty("instance", new StringSchema().format("uri"))
              .addProperty("requestId", new StringSchema().format("uuid"))
              .addProperty("errors", new MapSchema().additionalProperties(new StringSchema())));
      Map<String, String> erros =
          Map.of(
              "RequisicaoInvalida",
                  "ParÃ¢metros, campos ou tipos invÃ¡lidos; errors detalha os campos quando aplicÃ¡vel.",
              "NaoAutenticado",
                  "Token ausente, invÃ¡lido, expirado ou revogado; no login, credenciais invÃ¡lidas ou conta indisponÃ­vel.",
              "AcessoNegado",
                  "O perfil ou a responsabilidade sobre o recurso nÃ£o permite esta operaÃ§Ã£o.",
              "RecursoNaoEncontrado", "O recurso solicitado nÃ£o foi encontrado.",
              "ConflitoDados",
                  "Conflito de unicidade ou alteraÃ§Ã£o concorrente. Consulte o estado atual antes de repetir.",
              "RegraNegocio", "A operaÃ§Ã£o viola uma regra do processo de recrutamento.",
              "ErroInterno", "Erro interno. Informe o X-Request-Id ao suporte.");
      erros.forEach((nome, descricao) -> componentes.addResponses(nome, respostaErro(descricao)));
      if (documentacao.getPaths() == null) return;
      documentacao
          .getPaths()
          .forEach(
              (caminho, item) ->
                  item.readOperationsMap()
                      .forEach(
                          (metodo, operacao) -> {
                            var respostas = operacao.getResponses();
                            respostas.putIfAbsent("400", referenciaErro("RequisicaoInvalida"));
                            respostas.putIfAbsent("401", referenciaErro("NaoAutenticado"));
                            boolean login =
                                (caminho.equals("/api/v1/auth/login")
                                        || caminho.equals("/api/v1/auth/refresh"))
                                    && metodo == PathItem.HttpMethod.POST;
                            if (login) operacao.setSecurity(List.of());
                            else respostas.putIfAbsent("403", referenciaErro("AcessoNegado"));
                            if (caminho.contains("{"))
                              respostas.putIfAbsent("404", referenciaErro("RecursoNaoEncontrado"));
                            if (metodo != PathItem.HttpMethod.GET) {
                              respostas.putIfAbsent("409", referenciaErro("ConflitoDados"));
                              respostas.putIfAbsent("422", referenciaErro("RegraNegocio"));
                            }
                            respostas.putIfAbsent("500", referenciaErro("ErroInterno"));
                            respostas.forEach(
                                (codigo, resposta) -> {
                                  if (resposta.get$ref() == null)
                                    resposta.addHeaderObject(
                                        "X-Request-Id", cabecalhoRastreabilidade());
                                });
                            if (operacao.getParameters() != null)
                              operacao
                                  .getParameters()
                                  .forEach(
                                      parametro -> {
                                        if (parametro.getName().equals("page")) {
                                          parametro.setDescription("PÃ¡gina iniciada em zero.");
                                          if (parametro.getSchema() != null)
                                            parametro.getSchema().setMinimum(BigDecimal.ZERO);
                                        }
                                        if (parametro.getName().equals("size")) {
                                          parametro.setDescription(
                                              "Quantidade por pÃ¡gina, limitada a 100 registros.");
                                          if (parametro.getSchema() != null) {
                                            parametro.getSchema().setMinimum(BigDecimal.ONE);
                                            parametro.getSchema().setMaximum(new BigDecimal("100"));
                                          }
                                        }
                                        if (parametro.getName().equals("sort"))
                                          parametro.setDescription(
                                              "OrdenaÃ§Ã£o no formato campo,asc ou campo,desc. Use somente os campos permitidos pelo recurso.");
                                      });
                          }));
    };
  }

  private ApiResponse referenciaErro(String nome) {
    return new ApiResponse().$ref("#/components/responses/" + nome);
  }

  private ApiResponse respostaErro(String descricao) {
    return new ApiResponse()
        .description(descricao)
        .addHeaderObject("X-Request-Id", cabecalhoRastreabilidade())
        .content(
            new Content()
                .addMediaType(
                    "application/problem+json",
                    new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/DetalheErro"))));
  }

  private Header cabecalhoRastreabilidade() {
    return new Header()
        .description("Identificador desta requisiÃ§Ã£o para rastreamento e suporte.")
        .schema(new StringSchema().format("uuid"));
  }
}
