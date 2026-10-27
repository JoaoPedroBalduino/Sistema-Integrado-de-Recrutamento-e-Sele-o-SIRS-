package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.ApontamentoHoras;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApontamentoHorasRepository extends JpaRepository<ApontamentoHoras, Long> {
  List<ApontamentoHoras> findByVagaIdOrderByDataDesc(Long vagaId);
}
