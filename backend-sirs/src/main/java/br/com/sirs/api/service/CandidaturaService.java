package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;
import static br.com.sirs.api.model.enums.StatusCandidatura.*;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.entity.AvaliacaoCriterio;
import br.com.sirs.api.model.entity.Candidatura;
import br.com.sirs.api.model.entity.CriterioVaga;
import br.com.sirs.api.model.entity.HistoricoEtapa;
import br.com.sirs.api.model.entity.Proposta;
import br.com.sirs.api.model.enums.StatusCandidatura;
import br.com.sirs.api.model.enums.StatusVaga;
import br.com.sirs.api.model.request.RequisicaoAvaliacaoCandidatura;
import br.com.sirs.api.model.request.RequisicaoCandidatura;
import br.com.sirs.api.model.request.RequisicaoContratacao;
import br.com.sirs.api.model.request.RequisicaoDecisaoCandidatura;
import br.com.sirs.api.model.request.RequisicaoMovimentacaoCandidatura;
import br.com.sirs.api.model.request.RequisicaoProposta;
import br.com.sirs.api.model.response.RespostaAvaliacaoCriterio;
import br.com.sirs.api.model.response.RespostaCandidato;
import br.com.sirs.api.model.response.RespostaCandidatura;
import br.com.sirs.api.model.response.RespostaDetalheCandidatura;
import br.com.sirs.api.model.response.RespostaMovimentacaoCandidatura;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.model.response.RespostaProposta;
import br.com.sirs.api.model.value.NotaPonderada;
import br.com.sirs.api.repository.AvaliacaoCriterioRepository;
import br.com.sirs.api.repository.CandidatoRepository;
import br.com.sirs.api.repository.CandidaturaRepository;
import br.com.sirs.api.repository.CriterioVagaRepository;
import br.com.sirs.api.repository.DesignacaoRepository;
import br.com.sirs.api.repository.EtapaVagaRepository;
import br.com.sirs.api.repository.HistoricoEtapaRepository;
import br.com.sirs.api.repository.PropostaRepository;
import br.com.sirs.api.repository.UsuarioRepository;
import br.com.sirs.api.repository.VagaRepository;
import br.com.sirs.api.security.ContextoAcesso;
import br.com.sirs.api.security.PermissaoVaga;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidaturaService {
  private final CandidaturaRepository candidaturas;
  private final CandidatoRepository candidatos;
  private final VagaRepository vagas;
  private final EtapaVagaRepository etapas;
  private final CriterioVagaRepository criterios;
  private final AvaliacaoCriterioRepository avaliacoes;
  private final HistoricoEtapaRepository historicos;
  private final PropostaRepository propostas;
  private final DesignacaoRepository designacoes;
  private final CadastroApoioService cadastros;
  private final UsuarioRepository usuarios;
  private final ContextoAcesso acesso;
  private final PermissaoVaga permissaoVaga;
  private final CalculadoraNota calculadoraNota;
  private final RegistroAuditoriaService auditoria;
  private final Clock relogio;

  public RespostaPaginada<RespostaCandidatura> listar(
      Long vagaId, Long etapaId, StatusCandidatura status, Pageable paginacao) {
    acesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, GESTOR, AVALIADOR);
    Specification<Candidatura> especificacao =
        (entidade, consulta, construtor) -> construtor.conjunction();
    if (vagaId != null)
      especificacao =
          especificacao.and(
              (entidade, consulta, construtor) ->
                  construtor.equal(entidade.get("vaga").get("id"), vagaId));
    if (etapaId != null)
      especificacao =
          especificacao.and(
              (entidade, consulta, construtor) ->
                  construtor.equal(entidade.get("etapaAtual").get("id"), etapaId));
    if (status != null)
      especificacao =
          especificacao.and(
              (entidade, consulta, construtor) -> construtor.equal(entidade.get("status"), status));
    if (acesso.possuiPerfil(GESTOR))
      especificacao =
          especificacao.and(
              (entidade, consulta, construtor) ->
                  construtor.equal(
                      entidade.get("vaga").get("solicitante").get("id"), acesso.identificador()));
    if (acesso.possuiPerfil(AVALIADOR)) {
      var identificadores = designacoes.candidaturasDoUsuario(acesso.identificador());
      especificacao =
          especificacao.and(
              (entidade, consulta, construtor) ->
                  identificadores.isEmpty()
                      ? construtor.disjunction()
                      : entidade.get("id").in(identificadores));
    }
    var ordenacao =
        Sort.by(
                paginacao.getSort().stream()
                    .map(
                        ordem ->
                            ordem.getProperty().equals("notaFinal")
                                ? ordem.withProperty("notaParaOrdenacao")
                                : ordem)
                    .toList())
            .and(Sort.by("id"));
    var paginacaoOrdenada =
        PageRequest.of(paginacao.getPageNumber(), paginacao.getPageSize(), ordenacao);
    return RespostaPaginada.of(
        candidaturas.findAll(especificacao, paginacaoOrdenada).map(this::resumir));
  }

  @Transactional
  public RespostaDetalheCandidatura consultar(Long identificador) {
    var candidatura = buscar(identificador);
    permitirConsulta(candidatura);
    auditoria.registrarAuditoria("candidatura", identificador, "CONSULTA");
    var proposta =
        acesso.possuiPerfil(AVALIADOR)
            ? null
            : propostas.findByCandidaturaId(identificador).orElse(null);
    return new RespostaDetalheCandidatura(
        resumir(candidatura),
        RespostaCandidato.from(candidatura.getCandidato()),
        avaliacoes.findByCandidaturaId(identificador).stream()
            .map(RespostaAvaliacaoCriterio::from)
            .toList(),
        historicos.findByCandidaturaIdOrderByMovidoEmAsc(identificador).stream()
            .map(RespostaMovimentacaoCandidatura::from)
            .toList(),
        designacoes.listar(identificador),
        RespostaProposta.from(proposta));
  }

  private Candidatura buscar(Long identificador) {
    return candidaturas
        .findById(identificador)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Candidatura", identificador));
  }

  /** Toda alteracao bloqueia a vaga primeiro e serializa movimentacoes, notas e capacidade. */
  private Candidatura bloquear(Long identificador) {
    var candidatura = buscar(identificador);
    vagas.lockById(candidatura.getVaga().getId()).orElseThrow();
    return candidatura;
  }

  private void permitirConsulta(Candidatura candidatura) {
    if (acesso.possuiPerfil(AVALIADOR)) {
      if (!designacoes.existe(candidatura.getId(), acesso.identificador()))
        throw new AccessDeniedException("Candidatura nao designada");
    } else permissaoVaga.verificarConsultaSelecao(candidatura.getVaga());
  }

  private void permitirAlteracao(Candidatura candidatura) {
    permissaoVaga.verificarAlteracao(candidatura.getVaga());
    exigirAtiva(candidatura);
  }

  private void exigirAtiva(Candidatura candidatura) {
    if (candidatura.getVaga().getStatus() != StatusVaga.PUBLICADA
        || candidatura.getContratadaEm() != null
        || candidatura.getCandidato().getAnonimizadoEm() != null)
      throw new RegraNegocioException("Candidatura ou vaga indisponivel para alteracao.");
  }

  @Transactional
  public RespostaCandidatura cadastrar(RequisicaoCandidatura requisicao) {
    var vaga =
        vagas
            .lockById(requisicao.vagaId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga", requisicao.vagaId()));
    permissaoVaga.verificarAlteracao(vaga);
    if (vaga.getStatus() != StatusVaga.PUBLICADA)
      throw new RegraNegocioException("Somente vagas publicadas recebem candidaturas.");
    if (candidaturas.existsByVagaIdAndCandidatoId(vaga.getId(), requisicao.candidatoId()))
      throw new RegraNegocioException("Candidato ja inscrito nesta vaga.");
    var candidato =
        candidatos
            .buscarComBloqueio(requisicao.candidatoId())
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Candidato", requisicao.candidatoId()));
    if (candidato.getAnonimizadoEm() != null)
      throw new RegraNegocioException("Candidato anonimizado.");
    var canal = cadastros.canal(requisicao.canalId());
    if (vaga.getCanais().stream().noneMatch(canalVaga -> canalVaga.getId().equals(canal.getId())))
      throw new RegraNegocioException("Canal nao associado a vaga.");
    var primeiraEtapa =
        etapas.findByVagaIdOrderByOrdem(vaga.getId()).stream()
            .findFirst()
            .orElseThrow(() -> new RegraNegocioException("Vaga sem etapas."));
    var candidatura = new Candidatura();
    candidatura.setVaga(vaga);
    candidatura.setCandidato(candidato);
    candidatura.setCanal(canal);
    candidatura.setEtapaAtual(primeiraEtapa);
    candidatura.setCriadaEm(relogio.instant());
    candidato.setUltimaAtividadeEm(relogio.instant());
    candidaturas.saveAndFlush(candidatura);
    registrarMovimento(candidatura);
    auditoria.registrarAuditoria("candidatura", candidatura.getId(), "CRIACAO");
    return resumir(candidatura);
  }

  @Transactional
  public RespostaCandidatura movimentar(
      Long identificador, RequisicaoMovimentacaoCandidatura requisicao) {
    var candidatura = bloquear(identificador);
    permitirAlteracao(candidatura);
    if (candidatura.getStatus() == REPROVADO)
      throw new RegraNegocioException("Candidatura reprovada nao muda de etapa.");
    if (propostas.findByCandidaturaId(identificador).isPresent())
      throw new RegraNegocioException("Candidatura com proposta nao pode mudar de etapa.");
    var etapa =
        etapas
            .findById(requisicao.etapaId())
            .filter(
                etapaEncontrada ->
                    etapaEncontrada.getVagaId().equals(candidatura.getVaga().getId()))
            .orElseThrow(() -> new RegraNegocioException("Etapa nao pertence a vaga."));
    if (candidatura.getEtapaAtual().getId().equals(etapa.getId()))
      throw new RegraNegocioException("Candidatura ja esta nesta etapa.");
    candidatura.setEtapaAtual(etapa);
    registrarMovimento(candidatura);
    registrarAlteracao(candidatura);
    return resumir(candidatura);
  }

  @Transactional
  public RespostaCandidatura decidir(Long identificador, RequisicaoDecisaoCandidatura requisicao) {
    var candidatura = bloquear(identificador);
    permitirAlteracao(candidatura);
    if (candidatura.getStatus() != EM_AVALIACAO || requisicao.status() == EM_AVALIACAO)
      throw new RegraNegocioException("Transicao de candidatura invalida.");
    candidatura.setStatus(requisicao.status());
    registrarAlteracao(candidatura);
    return resumir(candidatura);
  }

  @Transactional
  public RespostaCandidatura avaliar(
      Long identificador, RequisicaoAvaliacaoCandidatura requisicao) {
    var candidatura = bloquear(identificador);
    if (acesso.possuiPerfil(AVALIADOR)) {
      permitirConsulta(candidatura);
      exigirAtiva(candidatura);
    } else permitirAlteracao(candidatura);
    if (candidatura.getStatus() != EM_AVALIACAO)
      throw new RegraNegocioException("Notas editaveis somente durante avaliacao.");
    var criteriosVaga = criterios.findByVagaIdOrderById(candidatura.getVaga().getId());
    var identificadoresValidos =
        criteriosVaga.stream().map(CriterioVaga::getId).collect(Collectors.toSet());
    Set<Long> identificadoresRecebidos = new HashSet<>();
    for (var nota : requisicao.notas()) {
      if (!identificadoresRecebidos.add(nota.criterioId())
          || !identificadoresValidos.contains(nota.criterioId()))
        throw new RegraNegocioException("Criterio repetido ou de outra vaga.");
      var avaliacao =
          avaliacoes
              .findByCandidaturaIdAndCriterioId(identificador, nota.criterioId())
              .orElseGet(AvaliacaoCriterio::new);
      avaliacao.setCandidaturaId(identificador);
      avaliacao.setCriterioId(nota.criterioId());
      avaliacao.setAvaliadorId(acesso.identificador());
      avaliacao.setNota(nota.nota());
      avaliacoes.save(avaliacao);
    }
    var notas =
        avaliacoes.findByCandidaturaId(identificador).stream()
            .collect(
                Collectors.toMap(AvaliacaoCriterio::getCriterioId, AvaliacaoCriterio::getNota));
    candidatura.setNotaFinal(
        calculadoraNota.calcular(
            criteriosVaga.stream()
                .map(
                    criterio ->
                        new NotaPonderada(
                            notas.getOrDefault(criterio.getId(), (short) 0), criterio.getPeso()))
                .toList()));
    registrarAlteracao(candidatura);
    return resumir(candidatura);
  }

  @Transactional
  public void designar(Long identificador, Long usuarioId, boolean remover) {
    var candidatura = bloquear(identificador);
    permitirAlteracao(candidatura);
    var avaliador =
        usuarios
            .findById(usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", usuarioId));
    if (!avaliador.isAtivo() || avaliador.isBloqueado() || avaliador.getPerfil() != AVALIADOR)
      throw new RegraNegocioException("Usuario deve ser um avaliador ativo.");
    if (remover) designacoes.remover(identificador, usuarioId);
    else designacoes.designar(identificador, usuarioId);
    auditoria.registrarAuditoria(
        "designacao_avaliador", identificador, remover ? "EXCLUSAO" : "CRIACAO");
  }

  private void exigirAprovacaoFinal(Candidatura candidatura) {
    var etapasVaga = etapas.findByVagaIdOrderByOrdem(candidatura.getVaga().getId());
    if (candidatura.getStatus() != APROVADO
        || etapasVaga.isEmpty()
        || !etapasVaga
            .get(etapasVaga.size() - 1)
            .getId()
            .equals(candidatura.getEtapaAtual().getId()))
      throw new RegraNegocioException("Proposta e contratacao exigem aprovacao na ultima etapa.");
  }

  @Transactional
  public RespostaProposta oferecer(Long identificador, RequisicaoProposta requisicao) {
    var candidatura = bloquear(identificador);
    permitirAlteracao(candidatura);
    exigirAprovacaoFinal(candidatura);
    if (propostas.findByCandidaturaId(identificador).isPresent())
      throw new RegraNegocioException("Candidatura ja possui proposta.");
    if (requisicao
        .dataProposta()
        .isBefore(candidatura.getCriadaEm().atZone(relogio.getZone()).toLocalDate()))
      throw new RegraNegocioException("Proposta anterior a candidatura.");
    var proposta = new Proposta();
    proposta.setCandidaturaId(identificador);
    proposta.setSalarioOfertado(requisicao.salarioOfertado());
    proposta.setDataProposta(requisicao.dataProposta());
    propostas.saveAndFlush(proposta);
    auditoria.registrarAuditoria("proposta", proposta.getId(), "CRIACAO");
    return RespostaProposta.from(proposta);
  }

  @Transactional
  public RespostaCandidatura contratar(Long identificador, RequisicaoContratacao requisicao) {
    var candidatura = bloquear(identificador);
    permitirAlteracao(candidatura);
    exigirAprovacaoFinal(candidatura);
    var proposta =
        propostas
            .findByCandidaturaId(identificador)
            .orElseThrow(
                () -> new RegraNegocioException("Registre a proposta antes da contratacao."));
    if (requisicao.dataAceite().isBefore(proposta.getDataProposta()))
      throw new RegraNegocioException("Aceite anterior a proposta.");
    if (candidaturas.countByVagaIdAndContratadaEmIsNotNull(candidatura.getVaga().getId())
        >= candidatura.getVaga().getPosicoes())
      throw new RegraNegocioException("Todas as posicoes da vaga foram preenchidas.");
    proposta.setDataAceite(requisicao.dataAceite());
    candidatura.setContratadaEm(
        requisicao.dataAceite().atStartOfDay(relogio.getZone()).toInstant());
    registrarAlteracao(candidatura);
    auditoria.registrarAuditoria("proposta", proposta.getId(), "ALTERACAO");
    return resumir(candidatura);
  }

  private void registrarMovimento(Candidatura candidatura) {
    var historico = new HistoricoEtapa();
    historico.setCandidaturaId(candidatura.getId());
    historico.setEtapaId(candidatura.getEtapaAtual().getId());
    historico.setUsuarioId(acesso.identificador());
    historico.setMovidoEm(relogio.instant());
    historicos.save(historico);
  }

  private void registrarAlteracao(Candidatura candidatura) {
    candidatura.getCandidato().setUltimaAtividadeEm(relogio.instant());
    auditoria.registrarAuditoria("candidatura", candidatura.getId(), "ALTERACAO");
  }

  private RespostaCandidatura resumir(Candidatura candidatura) {
    return new RespostaCandidatura(
        candidatura.getId(),
        candidatura.getVaga().getId(),
        candidatura.getVaga().getTitulo(),
        candidatura.getCandidato().getId(),
        candidatura.getCandidato().getNome(),
        candidatura.getCandidato().getEmail(),
        candidatura.getCanal().getId(),
        candidatura.getEtapaAtual() == null ? null : candidatura.getEtapaAtual().getId(),
        candidatura.getEtapaAtual() == null ? null : candidatura.getEtapaAtual().getNome(),
        candidatura.getStatus(),
        candidatura.getNotaFinal(),
        candidatura.getContratadaEm());
  }
}
