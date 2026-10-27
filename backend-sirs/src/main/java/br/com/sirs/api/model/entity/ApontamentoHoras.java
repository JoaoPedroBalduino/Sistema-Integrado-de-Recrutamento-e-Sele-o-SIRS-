package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "apontamento_horas")
@Getter
@Setter
@NoArgsConstructor
public class ApontamentoHoras extends EntidadeIdentificada {
  @Column(name = "vaga_id", nullable = false)
  private Long vagaId;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(nullable = false)
  private LocalDate data;

  @Column(nullable = false, precision = 4, scale = 2)
  private BigDecimal horas;

  @Column(length = 200)
  private String atividade;
}
