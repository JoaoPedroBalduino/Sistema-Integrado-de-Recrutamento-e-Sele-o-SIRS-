package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "avaliacao_criterio")
@Getter
@Setter
@NoArgsConstructor
public class AvaliacaoCriterio extends EntidadeIdentificada {
  @Column(name = "candidatura_id", nullable = false)
  private Long candidaturaId;

  @Column(name = "criterio_id", nullable = false)
  private Long criterioId;

  @Column(name = "avaliador_id", nullable = false)
  private Long avaliadorId;

  @Column(nullable = false)
  private short nota;
}
