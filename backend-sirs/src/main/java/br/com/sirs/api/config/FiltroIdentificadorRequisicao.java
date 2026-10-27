package br.com.sirs.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class FiltroIdentificadorRequisicao extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeiaFiltros)
      throws ServletException, IOException {
    String identificador = UUID.randomUUID().toString();
    MDC.put("requestId", identificador);
    resposta.setHeader("X-Request-Id", identificador);
    try {
      cadeiaFiltros.doFilter(requisicao, resposta);
    } finally {
      MDC.remove("requestId");
    }
  }
}
