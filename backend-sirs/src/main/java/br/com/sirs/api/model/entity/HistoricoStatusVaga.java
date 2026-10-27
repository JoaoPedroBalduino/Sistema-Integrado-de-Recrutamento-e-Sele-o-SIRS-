package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "historico_status_vaga")
@Getter
@Setter
@NoArgsConstructor
public class HistoricoStatusVaga extends EntidadeIdentificada {
  @Column(name = "vaga_id", nullable = false)
  private Long vagaId;

  @Column(name = "status_anterior", length = 20)
  private String statusAnterior;

  @Column(name = "status_novo", nullable = false, length = 20)
  private String statusNovo;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "alterado_em", nullable = false)
  private Instant alteradoEm;
}
