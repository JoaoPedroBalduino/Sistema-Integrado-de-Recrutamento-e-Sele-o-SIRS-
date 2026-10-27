package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.SessaoSupabase;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface SessaoSupabaseRepository extends JpaRepository<SessaoSupabase, UUID> {
  @Query(
      "select sessao from SessaoSupabase sessao join fetch sessao.usuario where sessao.id=:identificador and sessao.revogadaEm is null")
  Optional<SessaoSupabase> buscarAtiva(UUID identificador);

  @Modifying
  @Query(
      "update SessaoSupabase sessao set sessao.revogadaEm=:instante where sessao.usuario.id=:usuarioId and sessao.revogadaEm is null")
  void revogarTodas(Long usuarioId, Instant instante);
}
