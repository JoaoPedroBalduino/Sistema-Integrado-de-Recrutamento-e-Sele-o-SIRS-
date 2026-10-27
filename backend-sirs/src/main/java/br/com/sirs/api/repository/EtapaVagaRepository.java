package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.EtapaVaga;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtapaVagaRepository extends JpaRepository<EtapaVaga, Long> {
  List<EtapaVaga> findByVagaIdOrderByOrdem(Long vagaId);
}
