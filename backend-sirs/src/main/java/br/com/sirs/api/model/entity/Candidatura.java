package br.com.sirs.api.model.entity;

import br.com.sirs.api.model.enums.StatusCandidatura;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "candidatura")
@Getter
@Setter
@NoArgsConstructor
public class Candidatura extends EntidadeVersionada {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vaga_id")
  private Vaga vaga;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "candidato_id")
  private Candidato candidato;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "canal_id")
  private Canal canal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "etapa_atual_id")
  private EtapaVaga etapaAtual;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusCandidatura status = StatusCandidatura.EM_AVALIACAO;

  @Formula("coalesce(nota_final, -1)")
  private BigDecimal notaParaOrdenacao;

  @Column(name = "nota_final", precision = 5, scale = 4)
  private BigDecimal notaFinal;

  @Column(name = "contratada_em")
  private Instant contratadaEm;

  @Column(name = "criada_em", nullable = false)
  private Instant criadaEm;
}
