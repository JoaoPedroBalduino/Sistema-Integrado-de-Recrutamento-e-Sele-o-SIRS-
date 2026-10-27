package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "etapa_vaga")
@Getter
@Setter
@NoArgsConstructor
public class EtapaVaga extends EntidadeIdentificada {
  @Column(name = "vaga_id", nullable = false)
  private Long vagaId;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(columnDefinition = "text")
  private String descricao;

  @Column(nullable = false)
  private int ordem;
}
