package br.com.sirs.api.model.entity;

import jakarta.persistence.*;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class EntidadeVersionada extends EntidadeIdentificada {
  @Version private long version;
}
