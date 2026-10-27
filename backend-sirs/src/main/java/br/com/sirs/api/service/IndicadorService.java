package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;
import static br.com.sirs.api.model.enums.StatusCandidatura.*;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.entity.AvaliacaoCriterio;
import br.com.sirs.api.model.entity.Candidatura;
import br.com.sirs.api.model.entity.Vaga;
import br.com.sirs.api.model.enums.TipoConfiguracaoTemporal;
import br.com.sirs.api.model.response.RespostaIndicadorCanal;
import br.com.sirs.api.model.response.RespostaIndicadorCargo;
import br.com.sirs.api.model.response.RespostaIndicadorCriterio;
import br.com.sirs.api.model.response.RespostaIndicadorEtapa;
import br.com.sirs.api.model.response.RespostaIndicadorRecrutador;
import br.com.sirs.api.model.response.RespostaIndicadorVaga;
import br.com.sirs.api.model.response.RespostaResumoIndicadores;
import br.com.sirs.api.repository.AvaliacaoCriterioRepository;
import br.com.sirs.api.repository.CandidaturaRepository;
import br.com.sirs.api.repository.ConfiguracaoTemporalRepository;
import br.com.sirs.api.repository.CriterioVagaRepository;
import br.com.sirs.api.repository.EtapaVagaRepository;
import br.com.sirs.api.repository.IndicadorRepository;
import br.com.sirs.api.repository.VagaRepository;
import br.com.sirs.api.security.ContextoAcesso;
import br.com.sirs.api.security.PermissaoVaga;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndicadorService {
  private final IndicadorRepository indicadores;
  private final VagaRepository vagas;
  private final PermissaoVaga permissaoVaga;
  private final EtapaVagaRepository etapas;
  private final CriterioVagaRepository criterios;
  private final CandidaturaRepository candidaturas;
  private final AvaliacaoCriterioRepository avaliacoes;
  private final ConfiguracaoTemporalRepository configuracoes;
  private final CustoService custos;
  private final ContextoAcesso acesso;
  private final Clock relogio;

  public void validarPeriodo(LocalDate de, LocalDate ate) {
    if (ate.isBefore(de)
        || ChronoUnit.MONTHS.between(de.withDayOfMonth(1), ate.withDayOfMonth(1)) > 35)
      throw new RegraNegocioException("Informe um periodo valido de ate 36 meses.");
  }

  private Long gestorConsulta() {
    acesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, GESTOR, DIRETORIA);
    return acesso.possuiPerfil(GESTOR) ? acesso.identificador() : null;
  }

  public RespostaResumoIndicadores resumir(LocalDate de, LocalDate ate) {
    validarPeriodo(de, ate);
    Long gestorId = gestorConsulta();
    LocalDate referencia = ate.isBefore(LocalDate.now(relogio)) ? ate : LocalDate.now(relogio);
    Specification<Vaga> selecao =
        (entidade, consulta, construtor) ->
            construtor.between(entidade.get("dataFechamento"), de, ate);
    if (gestorId != null)
      selecao =
          selecao.and(
              (entidade, consulta, construtor) ->
                  construtor.equal(entidade.get("solicitante").get("id"), gestorId));
    var fechadas = vagas.findAll(selecao);
    var contagem = indicadores.vagasNoPrazo(de, ate, referencia, gestorId);
    BigDecimal totalCustos =
        fechadas.stream()
            .map(vaga -> custos.calcular(vaga, vaga.getDataFechamento(), true).total())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new RespostaResumoIndicadores(
        de,
        ate,
        fechadas.size(),
        contagem.quantidade(),
        contagem.atrasadas(),
        dividir(indicadores.horasFechadas(de, ate, gestorId), fechadas.size()),
        IndicadorRepository.percentual(
            contagem.quantidade() - contagem.atrasadas(), contagem.quantidade(), false),
        dividir(totalCustos, fechadas.size()),
        indicadores.mediaDiasAdmissao(de, ate, gestorId),
        configuracoes.vigentes(TipoConfiguracaoTemporal.META, ate));
  }

  public List<RespostaResumoIndicadores> historico(LocalDate de, LocalDate ate) {
    validarPeriodo(de, ate);
    gestorConsulta();
    List<RespostaResumoIndicadores> historico = new ArrayList<>();
    for (YearMonth mes = YearMonth.from(de);
        !mes.isAfter(YearMonth.from(ate));
        mes = mes.plusMonths(1)) {
      LocalDate inicio = mes.atDay(1).isBefore(de) ? de : mes.atDay(1);
      LocalDate fim = mes.atEndOfMonth().isAfter(ate) ? ate : mes.atEndOfMonth();
      historico.add(resumir(inicio, fim));
    }
    return historico;
  }

  public List<RespostaIndicadorCargo> cargos(LocalDate de, LocalDate ate) {
    validarPeriodo(de, ate);
    return indicadores.porCargo(
        de,
        ate,
        ate.isBefore(LocalDate.now(relogio)) ? ate : LocalDate.now(relogio),
        gestorConsulta());
  }

  public List<RespostaIndicadorRecrutador> recrutadores(LocalDate de, LocalDate ate) {
    validarPeriodo(de, ate);
    return indicadores.porRecrutador(de, ate, gestorConsulta());
  }

  public List<RespostaIndicadorCanal> canais(LocalDate de, LocalDate ate) {
    validarPeriodo(de, ate);
    acesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, DIRETORIA);
    return indicadores.porCanal(de, ate);
  }

  public RespostaIndicadorVaga vaga(Long vagaId) {
    gestorConsulta();
    var vaga =
        vagas.findById(vagaId).orElseThrow(() -> new RecursoNaoEncontradoException("Vaga", vagaId));
    permissaoVaga.verificarConsulta(vaga);
    var inscricoes = candidaturas.findByVagaId(vagaId);
    var etapasVaga = etapas.findByVagaIdOrderByOrdem(vagaId);
    List<RespostaIndicadorEtapa> funil = new ArrayList<>();
    long posteriores = 0;
    for (int indice = etapasVaga.size() - 1; indice >= 0; indice--) {
      var etapa = etapasVaga.get(indice);
      long quantidade =
          inscricoes.stream()
              .filter(
                  candidatura ->
                      candidatura.getEtapaAtual() != null
                          && candidatura.getEtapaAtual().getId().equals(etapa.getId()))
              .count();
      long passaram = posteriores + quantidade;
      funil.add(
          new RespostaIndicadorEtapa(
              etapa.getId(),
              etapa.getNome(),
              quantidade,
              passaram,
              indice == etapasVaga.size() - 1
                  ? null
                  : IndicadorRepository.percentual(posteriores, passaram, false)));
      posteriores = passaram;
    }
    Collections.reverse(funil);
    List<AvaliacaoCriterio> notas = new ArrayList<>();
    inscricoes.forEach(
        candidatura -> notas.addAll(avaliacoes.findByCandidaturaId(candidatura.getId())));
    var medias =
        criterios.findByVagaIdOrderById(vagaId).stream()
            .map(
                criterio -> {
                  var notasCriterio =
                      notas.stream()
                          .filter(nota -> nota.getCriterioId().equals(criterio.getId()))
                          .toList();
                  var soma =
                      notasCriterio.stream()
                          .map(nota -> BigDecimal.valueOf(nota.getNota()))
                          .reduce(BigDecimal.ZERO, BigDecimal::add);
                  return new RespostaIndicadorCriterio(
                      criterio.getId(), criterio.getNome(), dividir(soma, notasCriterio.size()));
                })
            .toList();
    var notasFinais =
        inscricoes.stream().map(Candidatura::getNotaFinal).filter(Objects::nonNull).toList();
    return new RespostaIndicadorVaga(
        vagaId,
        vaga.getDataFechamento() == null
            ? null
            : ChronoUnit.DAYS.between(vaga.getDataAbertura(), vaga.getDataFechamento()),
        dividir(BigDecimal.valueOf(inscricoes.size()), vaga.getPosicoes()),
        inscricoes.stream().filter(candidatura -> candidatura.getStatus() == APROVADO).count(),
        dividir(notasFinais.stream().reduce(BigDecimal.ZERO, BigDecimal::add), notasFinais.size()),
        funil,
        medias);
  }

  private BigDecimal dividir(BigDecimal numerador, long denominador) {
    return denominador == 0
        ? null
        : numerador.divide(BigDecimal.valueOf(denominador), 4, RoundingMode.HALF_UP);
  }
}
