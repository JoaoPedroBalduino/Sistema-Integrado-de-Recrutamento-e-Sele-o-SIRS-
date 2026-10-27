package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.SolicitacaoTitular;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SolicitacaoTitularRepository extends JpaRepository<SolicitacaoTitular, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select solicitacao from SolicitacaoTitular solicitacao where solicitacao.id=:identificador")
  Optional<SolicitacaoTitular> buscarComBloqueio(Long identificador);
}
