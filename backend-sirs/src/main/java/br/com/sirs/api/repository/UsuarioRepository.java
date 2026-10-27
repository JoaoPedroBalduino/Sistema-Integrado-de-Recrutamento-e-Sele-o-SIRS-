package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.Usuario;
import br.com.sirs.api.model.enums.Perfil;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  Optional<Usuario> findByEmail(String email);

  Page<Usuario> findByPerfilInAndAtivoTrueAndBloqueadoFalse(
      Collection<Perfil> perfis, Pageable paginacao);

  boolean existsByPerfil(Perfil perfil);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select usuario from Usuario usuario where usuario.email=:email")
  Optional<Usuario> lockByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select usuario from Usuario usuario where usuario.id=:identificador")
  Optional<Usuario> lockById(Long identificador);
}
