package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.Usuario;
import br.com.sirs.api.model.enums.Perfil;

public record RespostaUsuario(
    Long identificador,
    String nome,
    String email,
    Perfil perfil,
    boolean ativo,
    boolean bloqueado) {
  public static RespostaUsuario of(Usuario usuario) {
    return new RespostaUsuario(
        usuario.getId(),
        usuario.getNome(),
        usuario.getEmail(),
        usuario.getPerfil(),
        usuario.isAtivo(),
        usuario.isBloqueado());
  }
}
