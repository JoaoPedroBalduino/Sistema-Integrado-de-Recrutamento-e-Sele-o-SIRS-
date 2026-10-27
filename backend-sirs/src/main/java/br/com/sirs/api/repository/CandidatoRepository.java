package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.Candidato;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface CandidatoRepository
    extends JpaRepository<Candidato, Long>, JpaSpecificationExecutor<Candidato> {
  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long identificador);

  @Query(
      "select candidato.id from Candidato candidato where candidato.anonimizadoEm is null and candidato.ultimaAtividadeEm < :limite and not exists (select candidatura.id from Candidatura candidatura where candidatura.candidato.id=candidato.id and (candidatura.contratadaEm is not null or candidatura.status<>br.com.sirs.api.model.enums.StatusCandidatura.REPROVADO)) order by candidato.id")
  List<Long> findElegiveisRetencao(Instant limite, Pageable paginacao);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select candidato from Candidato candidato where candidato.id=:identificador")
  Optional<Candidato> buscarComBloqueio(Long identificador);
}
