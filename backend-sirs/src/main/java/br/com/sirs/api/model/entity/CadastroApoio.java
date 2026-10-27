package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import lombok.*;

@MappedSuperclass
@Getter
@Setter
public abstract class CadastroApoio extends EntidadeIdentificada {
  @Column(nullable = false, length = 100)
  private String nome;

  @Column(nullable = false)
  private boolean ativo = true;
}
