package br.com.sirs.api.repository;

import br.com.sirs.api.model.response.RespostaIndicadorCanal;
import br.com.sirs.api.model.response.RespostaIndicadorCargo;
import br.com.sirs.api.model.response.RespostaIndicadorRecrutador;
import br.com.sirs.api.model.value.ContagemVagasPrazo;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IndicadorRepository {

  private final JdbcTemplate bancoDados;

  public ContagemVagasPrazo vagasNoPrazo(
      LocalDate de, LocalDate ate, LocalDate referencia, Long gestorId) {
    return bancoDados.queryForObject(
        """
            select count(*) quantidade,count(*) filter(where (coalesce(data_fechamento,?::date)-data_abertura)>prazo_alvo_dias) atrasadas
            from vaga where data_abertura between ? and ? and data_publicacao is not null and status<>'CONGELADA'
            and (?::bigint is null or solicitante_id=?)
            """,
        (resultado, numeroLinha) ->
            new ContagemVagasPrazo(resultado.getLong("quantidade"), resultado.getLong("atrasadas")),
        referencia,
        de,
        ate,
        gestorId,
        gestorId);
  }

  public BigDecimal horasFechadas(LocalDate de, LocalDate ate, Long gestorId) {
    return bancoDados.queryForObject(
        "select coalesce(sum(apontamento.horas),0) from apontamento_horas apontamento join vaga on vaga.id=apontamento.vaga_id where vaga.data_fechamento between ? and ? and (?::bigint is null or vaga.solicitante_id=?)",
        BigDecimal.class,
        de,
        ate,
        gestorId,
        gestorId);
  }

  public BigDecimal mediaDiasAdmissao(LocalDate de, LocalDate ate, Long gestorId) {
    return bancoDados.queryForObject(
        "select avg(proposta.data_aceite-vaga.data_abertura) from proposta join candidatura on candidatura.id=proposta.candidatura_id join vaga on vaga.id=candidatura.vaga_id where proposta.data_aceite between ? and ? and (?::bigint is null or vaga.solicitante_id=?)",
        BigDecimal.class,
        de,
        ate,
        gestorId,
        gestorId);
  }

  public List<RespostaIndicadorCargo> porCargo(
      LocalDate de, LocalDate ate, LocalDate referencia, Long gestorId) {
    return bancoDados.query(
        """
            select cargo.id,cargo.nome,sum(case when vaga.status in ('PUBLICADA','CONGELADA') then greatest(vaga.posicoes-(select count(*) from candidatura where candidatura.vaga_id=vaga.id and contratada_em is not null),0) else 0 end) posicoes,
              sum((select count(*) from candidatura where candidatura.vaga_id=vaga.id)) candidatos,
              avg(coalesce(vaga.data_fechamento,?::date)-vaga.data_publicacao) dias,avg(vaga.investimento_divulgacao) investimento
            from vaga join cargo on cargo.id=vaga.cargo_id where vaga.data_abertura between ? and ? and (?::bigint is null or vaga.solicitante_id=?) group by cargo.id,cargo.nome order by cargo.nome
            """,
        (resultado, numeroLinha) ->
            new RespostaIndicadorCargo(
                resultado.getLong("id"),
                resultado.getString("nome"),
                resultado.getLong("posicoes"),
                resultado.getLong("candidatos"),
                resultado.getBigDecimal("dias"),
                resultado.getBigDecimal("investimento")),
        referencia,
        de,
        ate,
        gestorId,
        gestorId);
  }

  public List<RespostaIndicadorRecrutador> porRecrutador(
      LocalDate de, LocalDate ate, Long gestorId) {
    return bancoDados.query(
        """
            select usuario.id,usuario.nome,sum(vaga.posicoes) trabalhadas,
              sum(case when vaga.status='FECHADA' then (select count(*) from candidatura where candidatura.vaga_id=vaga.id and contratada_em is not null) else 0 end) preenchidas
            from vaga join usuario on usuario.id=vaga.recrutador_id where vaga.data_abertura between ? and ? and (?::bigint is null or vaga.solicitante_id=?) group by usuario.id,usuario.nome order by usuario.nome
            """,
        (resultado, numeroLinha) -> {
          long total = resultado.getLong("trabalhadas");
          long preenchidas = resultado.getLong("preenchidas");
          return new RespostaIndicadorRecrutador(
              resultado.getLong("id"),
              resultado.getString("nome"),
              total,
              preenchidas,
              percentual(preenchidas, total, false));
        },
        de,
        ate,
        gestorId,
        gestorId);
  }

  public List<RespostaIndicadorCanal> porCanal(LocalDate de, LocalDate ate) {
    var registros =
        bancoDados.queryForList(
            """
            select canal.id,canal.nome,coalesce((select sum(visitas) from visitas_canal where canal_id=canal.id and periodo between ? and ?),0) visitas,
              (select count(*) from candidatura where candidatura.canal_id=canal.id and contratada_em>=? and contratada_em<?) contratacoes
            from canal order by canal.nome
            """,
            de.withDayOfMonth(1),
            ate.withDayOfMonth(1),
            de.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant().atOffset(ZoneOffset.UTC),
            ate.plusDays(1)
                .atStartOfDay(ZoneId.of("America/Sao_Paulo"))
                .toInstant()
                .atOffset(ZoneOffset.UTC));
    long totalVisitas =
        registros.stream()
            .mapToLong(registro -> ((Number) registro.get("visitas")).longValue())
            .sum();
    return registros.stream()
        .map(
            registro -> {
              long visitas = ((Number) registro.get("visitas")).longValue();
              long contratacoes = ((Number) registro.get("contratacoes")).longValue();
              return new RespostaIndicadorCanal(
                  ((Number) registro.get("id")).longValue(),
                  (String) registro.get("nome"),
                  visitas,
                  contratacoes,
                  percentual(contratacoes, visitas, true),
                  percentual(contratacoes, totalVisitas, true));
            })
        .toList();
  }

  public static BigDecimal percentual(long numerador, long denominador, boolean zeroSemDivisor) {
    return denominador == 0
        ? (zeroSemDivisor ? BigDecimal.ZERO : null)
        : BigDecimal.valueOf(numerador)
            .multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP);
  }
}
