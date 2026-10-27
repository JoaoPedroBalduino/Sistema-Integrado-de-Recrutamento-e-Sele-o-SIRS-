package br.com.sirs.api.service;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.enums.StatusCandidatura;
import br.com.sirs.api.repository.CandidatoRepository;
import br.com.sirs.api.repository.CandidaturaRepository;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnonimizacaoService {
  private final CandidatoRepository candidatos;
  private final CandidaturaRepository candidaturas;
  private final RegistroAuditoriaService auditoria;
  private final Clock relogio;

  @Transactional
  public void anonimizar(Long identificador, boolean exigirRetencao) {
    var candidato =
        candidatos
            .buscarComBloqueio(identificador)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Candidato", identificador));
    if (candidato.getAnonimizadoEm() != null) return;
    if (exigirRetencao
        && candidato
            .getUltimaAtividadeEm()
            .isAfter(ZonedDateTime.now(relogio).minusMonths(12).toInstant())) return;
    if (candidaturas.findByCandidatoId(identificador).stream()
        .anyMatch(
            candidatura ->
                candidatura.getContratadaEm() != null
                    || candidatura.getStatus() != StatusCandidatura.REPROVADO)) {
      if (exigirRetencao) return;
      throw new RegraNegocioException(
          "Resolva candidaturas ativas e retencao de contratados antes da anonimizacao.");
    }
    candidato.setNome("Candidato anonimizado " + identificador);
    candidato.setEmail("anonimizado-" + identificador + "@invalid.local");
    candidato.setCurriculoUrl(null);
    candidato.setBaseLegal(null);
    candidato.setConsentimentoEm(null);
    candidato.setConsentimentoVersao(null);
    candidato.setAnonimizadoEm(relogio.instant());
    auditoria.registrarAuditoria("candidato", identificador, "ANONIMIZACAO");
  }
}
