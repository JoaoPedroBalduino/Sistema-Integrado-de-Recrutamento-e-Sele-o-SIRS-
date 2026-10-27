package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class EntidadeIdentificada {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
}
