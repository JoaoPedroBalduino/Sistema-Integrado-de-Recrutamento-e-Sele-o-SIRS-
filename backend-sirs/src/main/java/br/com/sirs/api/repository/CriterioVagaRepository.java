package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.CriterioVaga;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CriterioVagaRepository extends JpaRepository<CriterioVaga, Long> {
  List<CriterioVaga> findByVagaIdOrderById(Long vagaId);
}
