package br.com.sirs.api.repository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DesignacaoRepository {
  private final JdbcTemplate bancoDados;

  public boolean existe(Long candidaturaId, Long usuarioId) {
    return Boolean.TRUE.equals(
        bancoDados.queryForObject(
            "select exists(select 1 from designacao_avaliador where candidatura_id=? and usuario_id=?)",
            Boolean.class,
            candidaturaId,
            usuarioId));
  }

  public void designar(Long candidaturaId, Long usuarioId) {
    bancoDados.update(
        "insert into designacao_avaliador(candidatura_id,usuario_id) values (?,?) on conflict do nothing",
        candidaturaId,
        usuarioId);
  }

  public void remover(Long candidaturaId, Long usuarioId) {
    bancoDados.update(
        "delete from designacao_avaliador where candidatura_id=? and usuario_id=?",
        candidaturaId,
        usuarioId);
  }

  public List<Long> candidaturasDoUsuario(Long usuarioId) {
    return bancoDados.queryForList(
        "select candidatura_id from designacao_avaliador where usuario_id=?",
        Long.class,
        usuarioId);
  }

  public List<Long> listar(Long candidaturaId) {
    return bancoDados.queryForList(
        "select usuario_id from designacao_avaliador where candidatura_id=? order by usuario_id",
        Long.class,
        candidaturaId);
  }
}
