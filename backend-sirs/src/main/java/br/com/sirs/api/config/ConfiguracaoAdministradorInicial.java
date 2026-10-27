package br.com.sirs.api.config;

import br.com.sirs.api.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;

@Configuration
public class ConfiguracaoAdministradorInicial {
  @Bean
  ApplicationRunner criarAdministradorInicial(
      UsuarioService usuarios,
      @Value("${sirs.bootstrap.email}") String email,
      @Value("${sirs.bootstrap.password}") String senha) {
    return args -> {
      if (!email.isBlank()) {
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
          throw new IllegalArgumentException("ADMIN_EMAIL invalido");
        usuarios.criarAdministradorInicial(email, senha);
      }
    };
  }
}
