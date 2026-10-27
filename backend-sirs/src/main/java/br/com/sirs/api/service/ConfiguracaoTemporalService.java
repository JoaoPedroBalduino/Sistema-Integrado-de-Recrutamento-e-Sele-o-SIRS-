package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.enums.ChaveParametroCusto;
import br.com.sirs.api.model.enums.IndicadorMeta;
import br.com.sirs.api.model.enums.TipoConfiguracaoTemporal;
import br.com.sirs.api.model.request.RequisicaoConfiguracaoTemporal;
import br.com.sirs.api.model.request.RequisicaoEncerramentoVigencia;
import br.com.sirs.api.model.response.RespostaConfiguracaoTemporal;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.repository.ConfiguracaoTemporalRepository;
import br.com.sirs.api.security.ContextoAcesso;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfiguracaoTemporalService {

  private final ConfiguracaoTemporalRepository configuracoes;
  private final ContextoAcesso acesso;
  private final RegistroAuditoriaService auditoria;

  public RespostaPaginada<RespostaConfiguracaoTemporal> listar(
      TipoConfiguracaoTemporal tipo, Pageable paginacao) {
    acesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, DIRETORIA);
    return RespostaPaginada.of(
        new PageImpl<>(
            configuracoes.listar(tipo, paginacao.getPageSize(), paginacao.getOffset()),
            paginacao,
            configuracoes.contar(tipo)));
  }

  @Transactional
  public RespostaConfiguracaoTemporal cadastrar(
      TipoConfiguracaoTemporal tipo, RequisicaoConfiguracaoTemporal requisicao) {
    acesso.exigirPerfil(ADMIN, COORD_RH);
    String chave = requisicao.chave().strip().toUpperCase(Locale.ROOT);
    try {
      if (tipo == TipoConfiguracaoTemporal.PARAMETRO)
        ChaveParametroCusto.valueOf(chave).validar(requisicao.valor());
      else {
        IndicadorMeta.valueOf(chave);
        if (chave.equals("VAGAS_NO_PRAZO")
            && requisicao.valor().compareTo(new BigDecimal("100")) > 0)
          throw new RegraNegocioException("Percentual deve estar entre 0 e 100.");
      }
    } catch (IllegalArgumentException excecao) {
      throw new RegraNegocioException("Chave de configuracao desconhecida.");
    }
    validarPeriodo(requisicao.vigenciaInicio(), requisicao.vigenciaFim());
    configuracoes.bloquear(tipo, chave);
    if (configuracoes.sobrepoe(
        tipo, chave, requisicao.vigenciaInicio(), requisicao.vigenciaFim(), null))
      throw new RegraNegocioException(
          "Vigencia sobreposta. Encerre o registro anterior antes de cadastrar o proximo.");
    var registro =
        configuracoes.inserir(
            tipo, chave, requisicao.valor(), requisicao.vigenciaInicio(), requisicao.vigenciaFim());
    auditoria.registrarAuditoria(tipo.name(), registro.identificador(), "CRIACAO");
    return registro;
  }

  @Transactional
  public void encerrar(
      TipoConfiguracaoTemporal tipo,
      Long identificador,
      RequisicaoEncerramentoVigencia requisicao) {
    acesso.exigirPerfil(ADMIN, COORD_RH);
    var registro =
        configuracoes
            .buscar(tipo, identificador)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Configuracao", identificador));
    configuracoes.bloquear(tipo, registro.chave());
    validarPeriodo(registro.vigenciaInicio(), requisicao.vigenciaFim());
    if (configuracoes.sobrepoe(
        tipo, registro.chave(), registro.vigenciaInicio(), requisicao.vigenciaFim(), identificador))
      throw new RegraNegocioException("Vigencia sobreposta.");
    configuracoes.encerrar(tipo, identificador, requisicao.vigenciaFim());
    auditoria.registrarAuditoria(tipo.name(), identificador, "ALTERACAO");
  }

  private void validarPeriodo(LocalDate inicio, LocalDate fim) {
    if (fim != null && fim.isBefore(inicio))
      throw new RegraNegocioException("Fim da vigencia anterior ao inicio.");
  }
}
