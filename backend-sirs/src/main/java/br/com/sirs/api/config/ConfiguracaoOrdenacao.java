package br.com.sirs.api.config;

import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class ConfiguracaoOrdenacao implements WebMvcConfigurer {
  private static final Map<String, Set<String>> CAMPOS_PERMITIDOS =
      Map.of(
          "/api/v1/vagas",
              Set.of("id", "titulo", "status", "dataAbertura", "dataPublicacao", "dataFechamento"),
          "/api/v1/candidaturas", Set.of("id", "status", "notaFinal", "criadaEm"),
          "/api/v1/candidatos", Set.of("id", "nome", "email", "criadoEm"),
          "/api/v1/usuarios", Set.of("id", "nome", "email", "perfil"),
          "/api/v1/visitas-canais", Set.of("id", "periodo", "visitas"));

  @Override
  public void addInterceptors(InterceptorRegistry registro) {
    registro
        .addInterceptor(
            new HandlerInterceptor() {
              @Override
              public boolean preHandle(
                  HttpServletRequest requisicao, HttpServletResponse resposta, Object manipulador) {
                var ordenacoes = requisicao.getParameterValues("sort");
                if (ordenacoes == null) return true;
                var permitidos =
                    CAMPOS_PERMITIDOS.getOrDefault(
                        requisicao.getRequestURI(), Set.of("id", "nome"));
                for (var ordenacao : ordenacoes) {
                  var componentes = ordenacao.split(",", -1);
                  if (componentes.length > 2
                      || !permitidos.contains(componentes[0])
                      || (componentes.length == 2
                          && !Set.of("asc", "desc")
                              .contains(componentes[1].toLowerCase(Locale.ROOT))))
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Ordenacao invalida.");
                }
                return true;
              }
            })
        .addPathPatterns("/api/v1/**");
  }
}
