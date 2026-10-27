package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.HistoricoStatusVaga;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusVagaRepository extends JpaRepository<HistoricoStatusVaga, Long> {
  List<HistoricoStatusVaga> findByVagaIdOrderByAlteradoEmAsc(Long vagaId);
}
