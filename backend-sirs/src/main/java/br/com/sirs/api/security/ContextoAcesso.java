package br.com.sirs.api.security;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.model.entity.Usuario;
import br.com.sirs.api.model.enums.Perfil;
import br.com.sirs.api.repository.UsuarioRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContextoAcesso {
  private final UsuarioRepository usuarios;

  public Long identificador() {
    return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
  }

  public Usuario usuarioAtual() {
    return usuarios
        .findById(identificador())
        .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", identificador()));
  }

  public boolean possuiPerfil(Perfil... perfis) {
    var perfil = usuarioAtual().getPerfil();
    return Arrays.asList(perfis).contains(perfil);
  }

  public void exigirPerfil(Perfil... perfis) {
    if (!possuiPerfil(perfis)) throw new AccessDeniedException("Acesso negado");
  }
}
