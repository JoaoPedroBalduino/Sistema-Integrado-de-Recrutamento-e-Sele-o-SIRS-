package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.Vaga;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.EntityGraph;

public interface VagaRepository extends JpaRepository<Vaga, Long>, JpaSpecificationExecutor<Vaga> {
  @EntityGraph(attributePaths = {"cargo", "area"})
  Page<Vaga> findAll(Specification<Vaga> especificacao, Pageable paginacao);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select vaga from Vaga vaga where vaga.id=:identificador")
  Optional<Vaga> lockById(Long identificador);
}
