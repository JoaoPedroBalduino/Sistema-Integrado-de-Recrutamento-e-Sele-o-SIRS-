package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "historico_etapa")
@Getter
@Setter
@NoArgsConstructor
public class HistoricoEtapa extends EntidadeIdentificada {
  @Column(name = "candidatura_id", nullable = false)
  private Long candidaturaId;

  @Column(name = "etapa_id", nullable = false)
  private Long etapaId;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "movido_em", nullable = false)
  private Instant movidoEm;
}
