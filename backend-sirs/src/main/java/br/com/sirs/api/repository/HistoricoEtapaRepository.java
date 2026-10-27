package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.HistoricoEtapa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoEtapaRepository extends JpaRepository<HistoricoEtapa, Long> {
  List<HistoricoEtapa> findByCandidaturaIdOrderByMovidoEmAsc(Long candidaturaId);
}
