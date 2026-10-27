package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.Candidatura;
import br.com.sirs.api.model.enums.StatusCandidatura;
import br.com.sirs.api.model.value.ContagemCandidaturasVaga;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

public interface CandidaturaRepository
    extends JpaRepository<Candidatura, Long>, JpaSpecificationExecutor<Candidatura> {
  @EntityGraph(attributePaths = {"vaga", "candidato", "canal", "etapaAtual"})
  Page<Candidatura> findAll(Specification<Candidatura> especificacao, Pageable paginacao);

  @Query(
      "select candidatura.vaga.id as vagaId,count(candidatura) as quantidade,count(candidatura.contratadaEm) as contratadas from Candidatura candidatura where candidatura.vaga.id in :identificadores group by candidatura.vaga.id")
  List<ContagemCandidaturasVaga> contarPorVagas(Collection<Long> identificadores);

  long countByVagaId(Long vagaId);

  long countByVagaIdAndContratadaEmIsNotNull(Long vagaId);

  boolean existsByVagaIdAndCandidatoId(Long vagaId, Long candidatoId);

  boolean existsByCandidatoIdAndContratadaEmIsNotNull(Long candidatoId);

  boolean existsByCandidatoIdAndStatus(Long candidatoId, StatusCandidatura status);

  List<Candidatura> findByVagaId(Long vagaId);

  List<Candidatura> findByCandidatoId(Long candidatoId);
}
