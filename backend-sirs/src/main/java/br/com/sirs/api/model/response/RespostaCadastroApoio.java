package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.CadastroApoio;

public record RespostaCadastroApoio(Long identificador, String nome, boolean ativo) {
  public static RespostaCadastroApoio of(CadastroApoio cadastro) {
    return new RespostaCadastroApoio(cadastro.getId(), cadastro.getNome(), cadastro.isAtivo());
  }
}
