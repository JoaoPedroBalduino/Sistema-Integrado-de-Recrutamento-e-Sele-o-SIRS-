package br.com.sirs.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class ConfiguracaoPaginacao {
  @Bean
  PageableHandlerMethodArgumentResolverCustomizer limitarPaginacao() {
    return configuracao -> {
      configuracao.setMaxPageSize(100);
      configuracao.setFallbackPageable(PageRequest.of(0, 20));
    };
  }
}
