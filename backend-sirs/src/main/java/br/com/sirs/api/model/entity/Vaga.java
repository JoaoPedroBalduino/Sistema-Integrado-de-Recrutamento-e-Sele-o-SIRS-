package br.com.sirs.api.model.entity;

import br.com.sirs.api.model.enums.StatusVaga;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "vaga")
@Getter
@Setter
@NoArgsConstructor
public class Vaga extends EntidadeVersionada {
  @Column(nullable = false, length = 200)
  private String titulo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cargo_id")
  private Cargo cargo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "area_id")
  private Area area;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "solicitante_id")
  private Usuario solicitante;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recrutador_id")
  private Usuario recrutador;

  @Column(nullable = false)
  private int posicoes;

  @Column(length = 100)
  private String cidade;

  @Column(length = 2)
  private String estado;

  @Column(nullable = false)
  private boolean remoto;

  @Column(columnDefinition = "text")
  private String observacoes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusVaga status = StatusVaga.RASCUNHO;

  @Column(name = "data_abertura")
  private LocalDate dataAbertura;

  @Column(name = "data_publicacao")
  private LocalDate dataPublicacao;

  @Column(name = "data_fechamento")
  private LocalDate dataFechamento;

  @Column(name = "prazo_alvo_dias")
  private Integer prazoAlvoDias;

  @Column(name = "investimento_divulgacao", nullable = false, precision = 12, scale = 2)
  private BigDecimal investimentoDivulgacao = BigDecimal.ZERO;

  @ManyToMany
  @JoinTable(
      name = "vaga_canal",
      joinColumns = @JoinColumn(name = "vaga_id"),
      inverseJoinColumns = @JoinColumn(name = "canal_id"))
  private Set<Canal> canais = new HashSet<>();
}
