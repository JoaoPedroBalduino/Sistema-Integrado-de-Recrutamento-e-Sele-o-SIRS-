package br.com.sirs.api.model.entity;

import br.com.sirs.api.model.enums.TipoSolicitacao;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "solicitacao_titular")
@Getter
@Setter
@NoArgsConstructor
public class SolicitacaoTitular extends EntidadeIdentificada {
  @Column(name = "candidato_id", nullable = false)
  private Long candidatoId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoSolicitacao tipo;

  @Column(nullable = false, length = 20)
  private String status = "ABERTA";

  @Column(name = "solicitada_em", nullable = false)
  private Instant solicitadaEm;

  @Column(nullable = false)
  private LocalDate prazo;

  @Column(name = "concluida_em")
  private Instant concluidaEm;
}
