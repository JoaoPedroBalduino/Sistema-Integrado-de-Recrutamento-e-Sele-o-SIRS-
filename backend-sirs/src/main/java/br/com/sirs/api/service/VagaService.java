package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;
import static br.com.sirs.api.model.enums.StatusVaga.*;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.entity.ApontamentoHoras;
import br.com.sirs.api.model.entity.CriterioVaga;
import br.com.sirs.api.model.entity.EtapaVaga;
import br.com.sirs.api.model.entity.HistoricoStatusVaga;
import br.com.sirs.api.model.entity.Usuario;
import br.com.sirs.api.model.entity.Vaga;
import br.com.sirs.api.model.enums.Perfil;
import br.com.sirs.api.model.enums.StatusVaga;
import br.com.sirs.api.model.request.RequisicaoAlteracaoStatusVaga;
import br.com.sirs.api.model.request.RequisicaoApontamentoHoras;
import br.com.sirs.api.model.request.RequisicaoCadastroCriterio;
import br.com.sirs.api.model.request.RequisicaoCadastroEtapa;
import br.com.sirs.api.model.request.RequisicaoVaga;
import br.com.sirs.api.model.response.RespostaApontamentoHoras;
import br.com.sirs.api.model.response.RespostaCadastroApoio;
import br.com.sirs.api.model.response.RespostaCriterioVaga;
import br.com.sirs.api.model.response.RespostaDetalheVaga;
import br.com.sirs.api.model.response.RespostaEtapaVaga;
import br.com.sirs.api.model.response.RespostaHistoricoVaga;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.model.response.RespostaVaga;
import br.com.sirs.api.model.value.ContagemCandidaturasVaga;
import br.com.sirs.api.repository.ApontamentoHorasRepository;
import br.com.sirs.api.repository.CandidaturaRepository;
import br.com.sirs.api.repository.CriterioVagaRepository;
import br.com.sirs.api.repository.EtapaVagaRepository;
import br.com.sirs.api.repository.HistoricoStatusVagaRepository;
import br.com.sirs.api.repository.UsuarioRepository;
import br.com.sirs.api.repository.VagaRepository;
import br.com.sirs.api.security.ContextoAcesso;
import br.com.sirs.api.security.PermissaoVaga;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VagaService {
  private final VagaRepository vagas;
  private final EtapaVagaRepository etapas;
  private final CriterioVagaRepository criterios;
  private final HistoricoStatusVagaRepository historicos;
  private final ApontamentoHorasRepository horas;
  private final CadastroApoioService cadastros;
  private final UsuarioRepository usuarios;
  private final CandidaturaRepository candidaturas;
  private final ContextoAcesso contextoAcesso;
  private final PermissaoVaga permissoes;
  private final RegistroAuditoriaService registroAuditoria;
  private final Clock relogio;

  public Vaga buscarVaga(Long identificador) {
    return vagas
        .findById(identificador)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga", identificador));
  }

  public Vaga bloquearVaga(Long identificador) {
    return vagas
        .lockById(identificador)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga", identificador));
  }

  public RespostaPaginada<RespostaVaga> listar(
      StatusVaga status,
      Long areaId,
      Long cargoId,
      Long recrutadorId,
      LocalDate de,
      LocalDate ate,
      Pageable paginacao) {
    contextoAcesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, GESTOR, DIRETORIA);
    Specification<Vaga> especificacao =
        (requisicao, consulta, construtor) -> construtor.conjunction();
    if (status != null)
      especificacao =
          especificacao.and(
              (requisicao, consulta, construtor) ->
                  construtor.equal(requisicao.get("status"), status));
    if (areaId != null)
      especificacao =
          especificacao.and(
              (requisicao, consulta, construtor) ->
                  construtor.equal(requisicao.get("area").get("id"), areaId));
    if (cargoId != null)
      especificacao =
          especificacao.and(
              (requisicao, consulta, construtor) ->
                  construtor.equal(requisicao.get("cargo").get("id"), cargoId));
    if (recrutadorId != null)
      especificacao =
          especificacao.and(
              (requisicao, consulta, construtor) ->
                  construtor.equal(requisicao.get("recrutador").get("id"), recrutadorId));
    if (de != null)
      especificacao =
          especificacao.and(
              (requisicao, consulta, construtor) ->
                  construtor.greaterThanOrEqualTo(requisicao.get("dataAbertura"), de));
    if (ate != null)
      especificacao =
          especificacao.and(
              (requisicao, consulta, construtor) ->
                  construtor.lessThanOrEqualTo(requisicao.get("dataAbertura"), ate));
    if (de != null && ate != null && ate.isBefore(de))
      throw new RegraNegocioException("Periodo invalido.");
    if (contextoAcesso.possuiPerfil(GESTOR))
      especificacao =
          especificacao.and(
              (requisicao, consulta, construtor) ->
                  construtor.equal(
                      requisicao.get("solicitante").get("id"), contextoAcesso.identificador()));
    var pagina = vagas.findAll(especificacao, paginacao);
    var identificadores = pagina.getContent().stream().map(Vaga::getId).toList();
    var contagens =
        identificadores.isEmpty()
            ? Map.<Long, ContagemCandidaturasVaga>of()
            : candidaturas.contarPorVagas(identificadores).stream()
                .collect(
                    Collectors.toMap(ContagemCandidaturasVaga::getVagaId, contagem -> contagem));
    return RespostaPaginada.of(
        pagina.map(
            vaga -> {
              var contagem = contagens.get(vaga.getId());
              return montarRespostaVaga(
                  vaga,
                  contagem == null ? 0 : contagem.getQuantidade(),
                  contagem == null ? 0 : contagem.getContratadas());
            }));
  }

  public RespostaDetalheVaga detalhe(Long identificador) {
    var vaga = buscarVaga(identificador);
    permissoes.verificarConsulta(vaga);
    return new RespostaDetalheVaga(
        montarRespostaVaga(vaga),
        etapas.findByVagaIdOrderByOrdem(identificador).stream().map(RespostaEtapaVaga::of).toList(),
        criterios.findByVagaIdOrderById(identificador).stream()
            .map(RespostaCriterioVaga::of)
            .toList(),
        historicos.findByVagaIdOrderByAlteradoEmAsc(identificador).stream()
            .map(RespostaHistoricoVaga::of)
            .toList(),
        horas.findByVagaIdOrderByDataDesc(identificador).stream()
            .map(RespostaApontamentoHoras::of)
            .toList());
  }

  @Transactional
  public RespostaVaga cadastrar(RequisicaoVaga requisicao) {
    contextoAcesso.exigirPerfil(COORD_RH, RECRUTADOR, GESTOR);
    var vaga = new Vaga();
    preencherVaga(vaga, requisicao);
    permissoes.verificarAlteracao(vaga);
    vagas.saveAndFlush(vaga);
    registrarHistoricoStatus(vaga, null);
    registroAuditoria.registrarAuditoria("vaga", vaga.getId(), "CRIACAO");
    return montarRespostaVaga(vaga);
  }

  @Transactional
  public RespostaVaga atualizar(Long identificador, RequisicaoVaga requisicao) {
    var vaga = bloquearVaga(identificador);
    permissoes.verificarAlteracao(vaga);
    exigirRascunho(vaga);
    preencherVaga(vaga, requisicao);
    permissoes.verificarAlteracao(vaga);
    registroAuditoria.registrarAuditoria("vaga", identificador, "ALTERACAO");
    return montarRespostaVaga(vaga);
  }

  private void preencherVaga(Vaga vaga, RequisicaoVaga requisicao) {
    var recrutador = usuario(requisicao.recrutadorId(), RECRUTADOR, COORD_RH);
    var solicitante = usuario(requisicao.solicitanteId(), GESTOR, COORD_RH);
    vaga.setTitulo(requisicao.titulo().strip());
    vaga.setCargo(cadastros.cargo(requisicao.cargoId()));
    vaga.setArea(cadastros.area(requisicao.areaId()));
    vaga.setSolicitante(solicitante);
    vaga.setRecrutador(recrutador);
    vaga.setPosicoes(requisicao.posicoes());
    vaga.setCidade(requisicao.cidade());
    vaga.setEstado(requisicao.estado());
    vaga.setRemoto(requisicao.remoto());
    vaga.setObservacoes(requisicao.observacoes());
    vaga.setDataAbertura(
        requisicao.dataAbertura() == null ? LocalDate.now(relogio) : requisicao.dataAbertura());
    vaga.setPrazoAlvoDias(requisicao.prazoAlvoDias());
    vaga.setInvestimentoDivulgacao(requisicao.investimentoDivulgacao());
    vaga.getCanais().clear();
    requisicao
        .canalIds()
        .forEach(identificador -> vaga.getCanais().add(cadastros.canal(identificador)));
  }

  private Usuario usuario(Long identificador, Perfil... perfis) {
    var usuario =
        usuarios
            .findById(identificador)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", identificador));
    if (!usuario.isAtivo()
        || usuario.isBloqueado()
        || !Arrays.asList(perfis).contains(usuario.getPerfil()))
      throw new RegraNegocioException("Usuario inativo ou com perfil incompativel.");
    return usuario;
  }

  @Transactional
  public RespostaVaga alterarStatus(Long identificador, RequisicaoAlteracaoStatusVaga requisicao) {
    var vaga = bloquearVaga(identificador);
    if (vaga.getStatus() == EM_APROVACAO) contextoAcesso.exigirPerfil(COORD_RH, DIRETORIA);
    else permissoes.verificarAlteracao(vaga);
    if (!vaga.getStatus().permits(requisicao.status()))
      throw new RegraNegocioException(
          "Transicao de vaga invalida: " + vaga.getStatus() + " -> " + requisicao.status());
    if (requisicao.status() == EM_APROVACAO || requisicao.status() == PUBLICADA) {
      if (vaga.getCanais().isEmpty()
          || etapas.findByVagaIdOrderByOrdem(identificador).isEmpty()
          || criterios.findByVagaIdOrderById(identificador).isEmpty())
        throw new RegraNegocioException(
            "Defina canais, etapas e criterios antes de submeter/publicar.");
      cadastros.area(vaga.getArea().getId());
      cadastros.cargo(vaga.getCargo().getId());
      vaga.getCanais().forEach(criterio -> cadastros.canal(criterio.getId()));
      usuario(vaga.getRecrutador().getId(), RECRUTADOR, COORD_RH);
      usuario(vaga.getSolicitante().getId(), GESTOR, COORD_RH);
    }
    var statusAnterior = vaga.getStatus();
    if (requisicao.status() == PUBLICADA && vaga.getDataPublicacao() == null)
      vaga.setDataPublicacao(LocalDate.now(relogio));
    if (requisicao.status() == FECHADA) {
      var dataFechamento =
          requisicao.dataFechamento() == null
              ? LocalDate.now(relogio)
              : requisicao.dataFechamento();
      if (vaga.getDataPublicacao() == null
          || dataFechamento.isBefore(vaga.getDataPublicacao())
          || dataFechamento.isBefore(vaga.getDataAbertura()))
        throw new RegraNegocioException("Fechamento deve ser posterior a abertura e a publicacao.");
      vaga.setDataFechamento(dataFechamento);
    } else if (requisicao.dataFechamento() != null)
      throw new RegraNegocioException("Data de fechamento somente ao fechar a vaga.");
    vaga.setStatus(requisicao.status());
    registrarHistoricoStatus(vaga, statusAnterior);
    registroAuditoria.registrarAuditoria("vaga", identificador, "ALTERACAO");
    return montarRespostaVaga(vaga);
  }

  private void registrarHistoricoStatus(Vaga vaga, StatusVaga statusAnterior) {
    var apontamento = new HistoricoStatusVaga();
    apontamento.setVagaId(vaga.getId());
    apontamento.setStatusAnterior(statusAnterior == null ? null : statusAnterior.name());
    apontamento.setStatusNovo(vaga.getStatus().name());
    apontamento.setUsuarioId(contextoAcesso.identificador());
    apontamento.setAlteradoEm(relogio.instant());
    historicos.save(apontamento);
  }

  private void exigirRascunho(Vaga vaga) {
    if (vaga.getStatus() != RASCUNHO)
      throw new RegraNegocioException("Estrutura da vaga editavel somente em rascunho.");
  }

  @Transactional
  public RespostaEtapaVaga cadastrarEtapa(Long identificador, RequisicaoCadastroEtapa requisicao) {
    var vaga = bloquearVaga(identificador);
    permissoes.verificarAlteracao(vaga);
    exigirRascunho(vaga);
    if (etapas.findByVagaIdOrderByOrdem(identificador).stream()
        .anyMatch(etapa -> etapa.getOrdem() == requisicao.ordem()))
      throw new RegraNegocioException("Ordem de etapa ja utilizada.");
    var etapa = new EtapaVaga();
    etapa.setVagaId(identificador);
    etapa.setNome(requisicao.nome().strip());
    etapa.setDescricao(requisicao.descricao());
    etapa.setOrdem(requisicao.ordem());
    etapas.saveAndFlush(etapa);
    registroAuditoria.registrarAuditoria("etapa_vaga", etapa.getId(), "CRIACAO");
    return RespostaEtapaVaga.of(etapa);
  }

  @Transactional
  public RespostaCriterioVaga cadastrarCriterio(
      Long identificador, RequisicaoCadastroCriterio requisicao) {
    var vaga = bloquearVaga(identificador);
    permissoes.verificarAlteracao(vaga);
    exigirRascunho(vaga);
    var criterio = new CriterioVaga();
    criterio.setVagaId(identificador);
    criterio.setNome(requisicao.nome().strip());
    criterio.setDescricao(requisicao.descricao());
    criterio.setPeso(requisicao.peso());
    criterios.saveAndFlush(criterio);
    registroAuditoria.registrarAuditoria("criterio_vaga", criterio.getId(), "CRIACAO");
    return RespostaCriterioVaga.of(criterio);
  }

  @Transactional
  public void excluirEtapa(Long identificador, Long etapaId) {
    var vaga = bloquearVaga(identificador);
    permissoes.verificarAlteracao(vaga);
    exigirRascunho(vaga);
    var etapa =
        etapas
            .findById(etapaId)
            .filter(registroEncontrado -> registroEncontrado.getVagaId().equals(identificador))
            .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa", etapaId));
    etapas.delete(etapa);
    registroAuditoria.registrarAuditoria("etapa_vaga", etapaId, "EXCLUSAO");
  }

  @Transactional
  public void excluirCriterio(Long identificador, Long criterioId) {
    var vaga = bloquearVaga(identificador);
    permissoes.verificarAlteracao(vaga);
    exigirRascunho(vaga);
    var criterio =
        criterios
            .findById(criterioId)
            .filter(registroEncontrado -> registroEncontrado.getVagaId().equals(identificador))
            .orElseThrow(() -> new RecursoNaoEncontradoException("Criterio", criterioId));
    criterios.delete(criterio);
    registroAuditoria.registrarAuditoria("criterio_vaga", criterioId, "EXCLUSAO");
  }

  @Transactional
  public RespostaApontamentoHoras registrarHoras(
      Long identificador, RequisicaoApontamentoHoras requisicao) {
    var vaga = bloquearVaga(identificador);
    contextoAcesso.exigirPerfil(COORD_RH, RECRUTADOR);
    permissoes.verificarAlteracao(vaga);
    if (vaga.getStatus() == FECHADA || requisicao.data().isBefore(vaga.getDataAbertura()))
      throw new RegraNegocioException("Apontamento fora do ciclo da vaga.");
    var apontamento = new ApontamentoHoras();
    apontamento.setVagaId(identificador);
    apontamento.setUsuarioId(contextoAcesso.identificador());
    apontamento.setData(requisicao.data());
    apontamento.setHoras(requisicao.horas());
    apontamento.setAtividade(requisicao.atividade());
    horas.saveAndFlush(apontamento);
    registroAuditoria.registrarAuditoria("apontamento_horas", apontamento.getId(), "CRIACAO");
    return RespostaApontamentoHoras.of(apontamento);
  }

  private RespostaVaga montarRespostaVaga(Vaga vaga) {
    return montarRespostaVaga(
        vaga,
        candidaturas.countByVagaId(vaga.getId()),
        candidaturas.countByVagaIdAndContratadaEmIsNotNull(vaga.getId()));
  }

  private RespostaVaga montarRespostaVaga(
      Vaga vaga, long quantidadeCandidatos, long posicoesPreenchidas) {
    var dataFinal =
        vaga.getDataFechamento() == null ? LocalDate.now(relogio) : vaga.getDataFechamento();
    Long diasAberto =
        vaga.getDataPublicacao() == null
            ? null
            : ChronoUnit.DAYS.between(vaga.getDataPublicacao(), dataFinal);
    return new RespostaVaga(
        vaga.getId(),
        vaga.getVersion(),
        vaga.getTitulo(),
        RespostaCadastroApoio.of(vaga.getCargo()),
        RespostaCadastroApoio.of(vaga.getArea()),
        vaga.getSolicitante().getId(),
        vaga.getRecrutador().getId(),
        vaga.getPosicoes(),
        vaga.getCidade(),
        vaga.getEstado(),
        vaga.isRemoto(),
        vaga.getObservacoes(),
        vaga.getStatus(),
        vaga.getDataAbertura(),
        vaga.getDataPublicacao(),
        vaga.getDataFechamento(),
        vaga.getPrazoAlvoDias(),
        vaga.getInvestimentoDivulgacao(),
        vaga.getCanais().stream()
            .map(RespostaCadastroApoio::of)
            .sorted(Comparator.comparing(RespostaCadastroApoio::identificador))
            .toList(),
        quantidadeCandidatos,
        posicoesPreenchidas,
        diasAberto);
  }

  @Transactional
  public RespostaEtapaVaga atualizarEtapa(
      Long identificador, Long etapaId, RequisicaoCadastroEtapa requisicao) {
    var vaga = bloquearVaga(identificador);
    permissoes.verificarAlteracao(vaga);
    exigirRascunho(vaga);
    var etapa =
        etapas
            .findById(etapaId)
            .filter(etapaEncontrada -> etapaEncontrada.getVagaId().equals(identificador))
            .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa", etapaId));
    if (etapas.findByVagaIdOrderByOrdem(identificador).stream()
        .anyMatch(
            etapaExistente ->
                !etapaExistente.getId().equals(etapaId)
                    && etapaExistente.getOrdem() == requisicao.ordem()))
      throw new RegraNegocioException("Ordem de etapa ja utilizada.");
    etapa.setNome(requisicao.nome().strip());
    etapa.setDescricao(requisicao.descricao());
    etapa.setOrdem(requisicao.ordem());
    registroAuditoria.registrarAuditoria("etapa_vaga", etapaId, "ALTERACAO");
    return RespostaEtapaVaga.of(etapa);
  }

  @Transactional
  public RespostaCriterioVaga atualizarCriterio(
      Long identificador, Long criterioId, RequisicaoCadastroCriterio requisicao) {
    var vaga = bloquearVaga(identificador);
    permissoes.verificarAlteracao(vaga);
    exigirRascunho(vaga);
    var criterio =
        criterios
            .findById(criterioId)
            .filter(criterioEncontrado -> criterioEncontrado.getVagaId().equals(identificador))
            .orElseThrow(() -> new RecursoNaoEncontradoException("Criterio", criterioId));
    criterio.setNome(requisicao.nome().strip());
    criterio.setDescricao(requisicao.descricao());
    criterio.setPeso(requisicao.peso());
    registroAuditoria.registrarAuditoria("criterio_vaga", criterioId, "ALTERACAO");
    return RespostaCriterioVaga.of(criterio);
  }
}
