package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.VisitasCanal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitasCanalRepository extends JpaRepository<VisitasCanal, Long> {
  Optional<VisitasCanal> findByCanalIdAndPeriodo(Long canalId, LocalDate periodo);
}
