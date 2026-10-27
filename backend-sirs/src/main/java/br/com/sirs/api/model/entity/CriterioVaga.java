package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "criterio_vaga")
@Getter
@Setter
@NoArgsConstructor
public class CriterioVaga extends EntidadeIdentificada {
  @Column(name = "vaga_id", nullable = false)
  private Long vagaId;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(columnDefinition = "text")
  private String descricao;

  @Column(nullable = false)
  private short peso;
}
