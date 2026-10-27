package br.com.sirs.api.repository;

import br.com.sirs.api.model.enums.TipoConfiguracaoTemporal;
import br.com.sirs.api.model.response.RespostaConfiguracaoTemporal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConfiguracaoTemporalRepository {
  private final JdbcTemplate bancoDados;

  private RowMapper<RespostaConfiguracaoTemporal> mapeador(TipoConfiguracaoTemporal tipo) {
    return (resultado, numeroLinha) ->
        new RespostaConfiguracaoTemporal(
            resultado.getLong("id"),
            resultado.getString(tipo.getColuna()),
            resultado.getBigDecimal("valor"),
            resultado.getObject("vigencia_inicio", LocalDate.class),
            resultado.getObject("vigencia_fim", LocalDate.class));
  }

  public void bloquear(TipoConfiguracaoTemporal tipo, String chave) {
    bancoDados.queryForObject(
        "select pg_advisory_xact_lock(hashtextextended(?,0))",
        Object.class,
        tipo.getTabela() + ":" + chave);
  }

  public List<RespostaConfiguracaoTemporal> listar(
      TipoConfiguracaoTemporal tipo, int limite, long deslocamento) {
    return bancoDados.query(
        "select * from "
            + tipo.getTabela()
            + " order by "
            + tipo.getColuna()
            + ",vigencia_inicio desc limit ? offset ?",
        mapeador(tipo),
        limite,
        deslocamento);
  }

  public long contar(TipoConfiguracaoTemporal tipo) {
    return bancoDados.queryForObject("select count(*) from " + tipo.getTabela(), Long.class);
  }

  public Optional<RespostaConfiguracaoTemporal> buscar(
      TipoConfiguracaoTemporal tipo, Long identificador) {
    return bancoDados
        .query("select * from " + tipo.getTabela() + " where id=?", mapeador(tipo), identificador)
        .stream()
        .findFirst();
  }

  public boolean sobrepoe(
      TipoConfiguracaoTemporal tipo,
      String chave,
      LocalDate inicio,
      LocalDate fim,
      Long ignorarIdentificador) {
    return Boolean.TRUE.equals(
        bancoDados.queryForObject(
            "select exists(select 1 from "
                + tipo.getTabela()
                + " where "
                + tipo.getColuna()
                + "=? and id<>? and vigencia_inicio<=? and (vigencia_fim is null or vigencia_fim>=?))",
            Boolean.class,
            chave,
            ignorarIdentificador == null ? -1L : ignorarIdentificador,
            fim == null ? LocalDate.of(9999, 12, 31) : fim,
            inicio));
  }

  public RespostaConfiguracaoTemporal inserir(
      TipoConfiguracaoTemporal tipo,
      String chave,
      BigDecimal valor,
      LocalDate inicio,
      LocalDate fim) {
    return bancoDados.queryForObject(
        "insert into "
            + tipo.getTabela()
            + "("
            + tipo.getColuna()
            + ",valor,vigencia_inicio,vigencia_fim) values (?,?,?,?) returning *",
        mapeador(tipo),
        chave,
        valor,
        inicio,
        fim);
  }

  public void encerrar(TipoConfiguracaoTemporal tipo, Long identificador, LocalDate fim) {
    bancoDados.update(
        "update " + tipo.getTabela() + " set vigencia_fim=? where id=?", fim, identificador);
  }

  public Map<String, BigDecimal> vigentes(TipoConfiguracaoTemporal tipo, LocalDate referencia) {
    Map<String, BigDecimal> valores = new HashMap<>();
    bancoDados
        .query(
            "select distinct on ("
                + tipo.getColuna()
                + ") * from "
                + tipo.getTabela()
                + " where vigencia_inicio<=? and (vigencia_fim is null or vigencia_fim>=?) order by "
                + tipo.getColuna()
                + ",vigencia_inicio desc",
            mapeador(tipo),
            referencia,
            referencia)
        .forEach(registro -> valores.put(registro.chave(), registro.valor()));
    return valores;
  }

  public Map<String, BigDecimal> ajustesVaga(Long vagaId) {
    Map<String, BigDecimal> ajustes = new HashMap<>();
    bancoDados.query(
        "select chave,valor from vaga_parametro_custo where vaga_id=?",
        (RowCallbackHandler)
            resultado ->
                ajustes.put(resultado.getString("chave"), resultado.getBigDecimal("valor")),
        vagaId);
    return ajustes;
  }

  public void ajustarVaga(Long vagaId, String chave, BigDecimal valor) {
    bancoDados.update(
        "insert into vaga_parametro_custo(vaga_id,chave,valor) values (?,?,?) on conflict(vaga_id,chave) do update set valor=excluded.valor",
        vagaId,
        chave,
        valor);
  }
}
