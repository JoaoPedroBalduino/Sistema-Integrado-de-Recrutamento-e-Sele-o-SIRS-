package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "sessao_supabase")
@Getter
@Setter
@NoArgsConstructor
public class SessaoSupabase {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  @Column(name = "criada_em", nullable = false)
  private Instant criadaEm;

  @Column(name = "revogada_em")
  private Instant revogadaEm;
}
