package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "proposta")
@Getter
@Setter
@NoArgsConstructor
public class Proposta extends EntidadeIdentificada {
  @Column(name = "candidatura_id", nullable = false)
  private Long candidaturaId;

  @Column(name = "salario_ofertado", nullable = false, precision = 12, scale = 2)
  private BigDecimal salarioOfertado;

  @Column(name = "data_proposta", nullable = false)
  private LocalDate dataProposta;

  @Column(name = "data_aceite")
  private LocalDate dataAceite;
}
