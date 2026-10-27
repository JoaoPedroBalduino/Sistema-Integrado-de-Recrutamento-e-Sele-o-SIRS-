package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.Proposta;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {
  Optional<Proposta> findByCandidaturaId(Long candidaturaId);
}
