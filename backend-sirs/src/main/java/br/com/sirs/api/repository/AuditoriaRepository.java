package br.com.sirs.api.repository;

import br.com.sirs.api.model.response.RespostaAuditoria;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditoriaRepository {

  private final JdbcTemplate bancoDados;

  public List<RespostaAuditoria> listar(int limite, long deslocamento) {
    return bancoDados.query(
        "select id,usuario_id,entidade,entidade_id,acao,ocorrida_em from auditoria order by ocorrida_em desc,id desc limit ? offset ?",
        (resultado, numeroLinha) ->
            new RespostaAuditoria(
                resultado.getLong("id"),
                resultado.getObject("usuario_id", Long.class),
                resultado.getString("entidade"),
                resultado.getObject("entidade_id", Long.class),
                resultado.getString("acao"),
                resultado.getTimestamp("ocorrida_em").toInstant()),
        limite,
        deslocamento);
  }

  public long contar() {
    return bancoDados.queryForObject("select count(*) from auditoria", Long.class);
  }
}
