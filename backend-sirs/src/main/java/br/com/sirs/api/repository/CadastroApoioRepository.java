package br.com.sirs.api.repository;

import br.com.sirs.api.model.entity.CadastroApoio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CadastroApoioRepository<T extends CadastroApoio> extends JpaRepository<T, Long> {
  boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long identificador);
}
