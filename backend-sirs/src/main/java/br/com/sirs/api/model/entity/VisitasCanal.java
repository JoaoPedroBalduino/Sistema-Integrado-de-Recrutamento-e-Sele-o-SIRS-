package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "visitas_canal")
@Getter
@Setter
@NoArgsConstructor
public class VisitasCanal extends EntidadeIdentificada {
  @Column(name = "canal_id", nullable = false)
  private Long canalId;

  @Column(nullable = false)
  private LocalDate periodo;

  @Column(nullable = false)
  private int visitas;
}
