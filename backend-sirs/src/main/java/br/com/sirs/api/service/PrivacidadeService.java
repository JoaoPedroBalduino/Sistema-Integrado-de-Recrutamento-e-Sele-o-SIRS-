package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.entity.SolicitacaoTitular;
import br.com.sirs.api.model.enums.TipoSolicitacao;
import br.com.sirs.api.model.request.RequisicaoSolicitacaoTitular;
import br.com.sirs.api.model.response.RespostaCandidato;
import br.com.sirs.api.model.response.RespostaExportacaoTitular;
import br.com.sirs.api.model.response.RespostaInscricaoTitular;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.model.response.RespostaSolicitacaoTitular;
import br.com.sirs.api.repository.CandidatoRepository;
import br.com.sirs.api.repository.CandidaturaRepository;
import br.com.sirs.api.repository.SolicitacaoTitularRepository;
import br.com.sirs.api.security.ContextoAcesso;
import jakarta.validation.constraints.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrivacidadeService {

  private final SolicitacaoTitularRepository solicitacoes;
  private final CandidatoRepository candidatos;
  private final CandidaturaRepository candidaturas;
  private final AnonimizacaoService anonimizacao;
  private final ContextoAcesso acesso;
  private final RegistroAuditoriaService auditoria;
  private final Clock relogio;

  public RespostaPaginada<RespostaSolicitacaoTitular> listar(Pageable paginacao) {
    acesso.exigirPerfil(ADMIN, COORD_RH);
    return RespostaPaginada.of(
        solicitacoes.findAll(paginacao).map(RespostaSolicitacaoTitular::from));
  }

  @Transactional
  public RespostaSolicitacaoTitular cadastrar(RequisicaoSolicitacaoTitular requisicao) {
    acesso.exigirPerfil(ADMIN);
    if (!candidatos.existsById(requisicao.candidatoId()))
      throw new RecursoNaoEncontradoException("Candidato", requisicao.candidatoId());
    var solicitacao = new SolicitacaoTitular();
    solicitacao.setCandidatoId(requisicao.candidatoId());
    solicitacao.setTipo(requisicao.tipo());
    solicitacao.setSolicitadaEm(relogio.instant());
    solicitacao.setPrazo(LocalDate.now(relogio).plusDays(15));
    solicitacoes.saveAndFlush(solicitacao);
    auditoria.registrarAuditoria("solicitacao_titular", solicitacao.getId(), "CRIACAO");
    return RespostaSolicitacaoTitular.from(solicitacao);
  }

  @Transactional
  public RespostaSolicitacaoTitular concluir(Long identificador) {
    acesso.exigirPerfil(ADMIN);
    var solicitacao =
        solicitacoes
            .buscarComBloqueio(identificador)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao", identificador));
    if (solicitacao.getConcluidaEm() != null)
      throw new RegraNegocioException("Solicitacao ja concluida.");
    if (solicitacao.getTipo() == TipoSolicitacao.ANONIMIZACAO)
      anonimizacao.anonimizar(solicitacao.getCandidatoId(), false);
    solicitacao.setStatus("CONCLUIDA");
    solicitacao.setConcluidaEm(relogio.instant());
    auditoria.registrarAuditoria("solicitacao_titular", identificador, "ALTERACAO");
    return RespostaSolicitacaoTitular.from(solicitacao);
  }

  @Transactional
  public RespostaExportacaoTitular exportar(Long candidatoId) {
    acesso.exigirPerfil(ADMIN);
    var candidato =
        candidatos
            .findById(candidatoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Candidato", candidatoId));
    var inscricoes =
        candidaturas.findByCandidatoId(candidatoId).stream()
            .map(
                candidatura ->
                    new RespostaInscricaoTitular(
                        candidatura.getId(),
                        candidatura.getVaga().getId(),
                        candidatura.getStatus(),
                        candidatura.getContratadaEm()))
            .toList();
    auditoria.registrarAuditoria("candidato", candidatoId, "CONSULTA");
    return new RespostaExportacaoTitular(RespostaCandidato.from(candidato), inscricoes);
  }
}
