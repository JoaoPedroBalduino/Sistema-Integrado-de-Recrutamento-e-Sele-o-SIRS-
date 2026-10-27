package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.AvaliacaoCriterio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoCriterioRepository extends JpaRepository<AvaliacaoCriterio, Long> {
  List<AvaliacaoCriterio> findByCandidaturaId(Long candidaturaId);

  Optional<AvaliacaoCriterio> findByCandidaturaIdAndCriterioId(Long candidaturaId, Long criterioId);
}
