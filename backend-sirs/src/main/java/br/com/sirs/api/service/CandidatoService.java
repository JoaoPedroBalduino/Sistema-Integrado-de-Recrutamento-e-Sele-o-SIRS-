package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.entity.Candidato;
import br.com.sirs.api.model.entity.Candidatura;
import br.com.sirs.api.model.enums.BaseLegal;
import br.com.sirs.api.model.request.RequisicaoCandidato;
import br.com.sirs.api.model.response.RespostaCandidato;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.repository.CandidatoRepository;
import br.com.sirs.api.security.ContextoAcesso;
import java.time.Clock;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidatoService {
  private final CandidatoRepository candidatos;
  private final ContextoAcesso acesso;
  private final RegistroAuditoriaService auditoria;
  private final Clock relogio;

  public Candidato buscar(Long identificador) {
    return candidatos
        .findById(identificador)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Candidato", identificador));
  }

  private Specification<Candidato> permissaoConsulta() {
    acesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, GESTOR);
    return (entidade, consulta, construtor) -> {
      if (!acesso.possuiPerfil(GESTOR)) return construtor.conjunction();
      var subconsulta = consulta.subquery(Long.class);
      var candidatura = subconsulta.from(Candidatura.class);
      subconsulta
          .select(candidatura.get("id"))
          .where(
              construtor.equal(candidatura.get("candidato").get("id"), entidade.get("id")),
              construtor.equal(
                  candidatura.get("vaga").get("solicitante").get("id"), acesso.identificador()));
      return construtor.exists(subconsulta);
    };
  }

  public RespostaPaginada<RespostaCandidato> listar(String pesquisa, Pageable paginacao) {
    var permissao = permissaoConsulta();
    if (pesquisa != null && !pesquisa.isBlank()) {
      String termo =
          "%"
              + pesquisa
                  .toLowerCase(Locale.ROOT)
                  .replace("\\", "\\\\")
                  .replace("%", "\\%")
                  .replace("_", "\\_")
              + "%";
      permissao =
          permissao.and(
              (entidade, consulta, construtor) ->
                  construtor.or(
                      construtor.like(construtor.lower(entidade.get("nome")), termo, '\\'),
                      construtor.like(construtor.lower(entidade.get("email")), termo, '\\')));
    }
    return RespostaPaginada.of(
        candidatos.findAll(permissao, paginacao).map(RespostaCandidato::from));
  }

  @Transactional
  public RespostaCandidato consultar(Long identificador) {
    var permissao =
        permissaoConsulta()
            .and(
                (entidade, consulta, construtor) ->
                    construtor.equal(entidade.get("id"), identificador));
    var candidato =
        candidatos
            .findOne(permissao)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Candidato", identificador));
    auditoria.registrarAuditoria("candidato", identificador, "CONSULTA");
    return RespostaCandidato.from(candidato);
  }

  @Transactional
  public RespostaCandidato cadastrar(RequisicaoCandidato requisicao) {
    acesso.exigirPerfil(COORD_RH, RECRUTADOR);
    var candidato = new Candidato();
    preencher(candidato, requisicao);
    candidato.setCriadoEm(relogio.instant());
    candidato.setUltimaAtividadeEm(relogio.instant());
    candidatos.saveAndFlush(candidato);
    auditoria.registrarAuditoria("candidato", candidato.getId(), "CRIACAO");
    return RespostaCandidato.from(candidato);
  }

  @Transactional
  public RespostaCandidato atualizar(Long identificador, RequisicaoCandidato requisicao) {
    acesso.exigirPerfil(COORD_RH, RECRUTADOR);
    var candidato =
        candidatos
            .buscarComBloqueio(identificador)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Candidato", identificador));
    if (candidato.getAnonimizadoEm() != null)
      throw new RegraNegocioException("Candidato anonimizado nao pode ser reidentificado.");
    preencher(candidato, requisicao);
    candidato.setUltimaAtividadeEm(relogio.instant());
    auditoria.registrarAuditoria("candidato", identificador, "ALTERACAO");
    return RespostaCandidato.from(candidato);
  }

  private void preencher(Candidato candidato, RequisicaoCandidato requisicao) {
    if (requisicao.baseLegal() == BaseLegal.CONSENTIMENTO
        && (requisicao.versaoConsentimento() == null || requisicao.versaoConsentimento().isBlank()))
      throw new RegraNegocioException("Informe a versao do termo de consentimento.");
    String email = AutenticacaoService.normalizarEmail(requisicao.email());
    if (candidatos.existsByEmailIgnoreCaseAndIdNot(
        email, candidato.getId() == null ? -1L : candidato.getId()))
      throw new RegraNegocioException("E-mail de candidato ja cadastrado.");
    candidato.setNome(requisicao.nome().strip());
    candidato.setEmail(email);
    candidato.setCurriculoUrl(requisicao.curriculoUrl());
    candidato.setBaseLegal(requisicao.baseLegal());
    candidato.setConsentimentoEm(requisicao.dataColeta());
    candidato.setConsentimentoVersao(requisicao.versaoConsentimento());
  }
}
