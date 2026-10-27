package br.com.sirs.api.model.entity;

import br.com.sirs.api.model.enums.Perfil;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario extends EntidadeVersionada {
  @Column(name = "supabase_usuario_id", unique = true)
  private java.util.UUID supabaseUsuarioId;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(name = "senha_hash", nullable = false, length = 255)
  private String senhaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Perfil perfil;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "tentativas_falhas", nullable = false)
  private int tentativasFalhas;

  @Column(nullable = false)
  private boolean bloqueado;

  @Column(name = "token_version", nullable = false)
  private long tokenVersion;
}
