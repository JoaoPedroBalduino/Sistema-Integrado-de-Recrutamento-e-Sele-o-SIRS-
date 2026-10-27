package br.com.sirs.api.model.entity;

import br.com.sirs.api.model.enums.BaseLegal;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "candidato")
@Getter
@Setter
@NoArgsConstructor
public class Candidato extends EntidadeVersionada {
  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(name = "curriculo_url", length = 500)
  private String curriculoUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_legal", length = 50)
  private BaseLegal baseLegal;

  @Column(name = "consentimento_em")
  private Instant consentimentoEm;

  @Column(name = "consentimento_versao", length = 20)
  private String consentimentoVersao;

  @Column(name = "anonimizado_em")
  private Instant anonimizadoEm;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm;

  @Column(name = "ultima_atividade_em", nullable = false)
  private Instant ultimaAtividadeEm;
}
