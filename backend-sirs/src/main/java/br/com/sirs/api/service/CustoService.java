package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.entity.Vaga;
import br.com.sirs.api.model.enums.ChaveParametroCusto;
import br.com.sirs.api.model.enums.StatusVaga;
import br.com.sirs.api.model.enums.TipoConfiguracaoTemporal;
import br.com.sirs.api.model.response.RespostaEstimativaCusto;
import br.com.sirs.api.repository.ConfiguracaoTemporalRepository;
import br.com.sirs.api.repository.VagaRepository;
import br.com.sirs.api.security.ContextoAcesso;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustoService {
  private final ConfiguracaoTemporalRepository configuracoes;
  private final VagaRepository vagas;
  private final ContextoAcesso acesso;
  private final CalculadoraCusto calculadora;
  private final RegistroAuditoriaService auditoria;

  public RespostaEstimativaCusto consultar(
      Long vagaId, LocalDate referencia, boolean investimentoRealizado) {
    acesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, DIRETORIA);
    var vaga =
        vagas.findById(vagaId).orElseThrow(() -> new RecursoNaoEncontradoException("Vaga", vagaId));
    return calcular(vaga, referencia, investimentoRealizado);
  }

  public RespostaEstimativaCusto calcular(
      Vaga vaga, LocalDate referencia, boolean investimentoRealizado) {
    Map<ChaveParametroCusto, BigDecimal> valores = new EnumMap<>(ChaveParametroCusto.class);
    var vigentes = configuracoes.vigentes(TipoConfiguracaoTemporal.PARAMETRO, referencia);
    for (var chave : ChaveParametroCusto.values()) {
      var valor = vigentes.get(chave.name());
      if (valor == null) valor = vigentes.get(chave.name().toLowerCase(Locale.ROOT));
      if (valor != null) valores.put(chave, valor);
    }
    configuracoes
        .ajustesVaga(vaga.getId())
        .forEach((chave, valor) -> valores.put(ChaveParametroCusto.valueOf(chave), valor));
    return calculadora.calcular(
        valores, investimentoRealizado ? vaga.getInvestimentoDivulgacao() : null);
  }

  @Transactional
  public void ajustar(Long vagaId, Map<ChaveParametroCusto, BigDecimal> ajustes) {
    acesso.exigirPerfil(COORD_RH);
    var vaga =
        vagas.lockById(vagaId).orElseThrow(() -> new RecursoNaoEncontradoException("Vaga", vagaId));
    if (vaga.getStatus() == StatusVaga.FECHADA)
      throw new RegraNegocioException("Custos de vaga fechada nao podem ser alterados.");
    ajustes.forEach(
        (chave, valor) -> {
          chave.validar(valor);
          configuracoes.ajustarVaga(vagaId, chave.name(), valor);
        });
    auditoria.registrarAuditoria("vaga_parametro_custo", vagaId, "ALTERACAO");
  }
}
