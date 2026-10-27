package br.com.sirs.api.controller;

import br.com.sirs.api.model.enums.TipoConfiguracaoTemporal;
import br.com.sirs.api.model.request.RequisicaoConfiguracaoTemporal;
import br.com.sirs.api.model.request.RequisicaoEncerramentoVigencia;
import br.com.sirs.api.model.response.RespostaConfiguracaoTemporal;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.service.ConfiguracaoTemporalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoTemporalController {
  private final ConfiguracaoTemporalService configuracoes;

  @GetMapping("/parametros-custo")
  public RespostaPaginada<RespostaConfiguracaoTemporal> listarParametros(Pageable paginacao) {
    return configuracoes.listar(TipoConfiguracaoTemporal.PARAMETRO, paginacao);
  }

  @GetMapping("/metas")
  public RespostaPaginada<RespostaConfiguracaoTemporal> listarMetas(Pageable paginacao) {
    return configuracoes.listar(TipoConfiguracaoTemporal.META, paginacao);
  }

  @PostMapping("/parametros-custo")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaConfiguracaoTemporal cadastrarParametro(
      @Valid @RequestBody RequisicaoConfiguracaoTemporal requisicao) {
    return configuracoes.cadastrar(TipoConfiguracaoTemporal.PARAMETRO, requisicao);
  }

  @PostMapping("/metas")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaConfiguracaoTemporal cadastrarMeta(
      @Valid @RequestBody RequisicaoConfiguracaoTemporal requisicao) {
    return configuracoes.cadastrar(TipoConfiguracaoTemporal.META, requisicao);
  }

  @PatchMapping("/parametros-custo/{identificador}/vigencia")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void encerrarParametro(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoEncerramentoVigencia requisicao) {
    configuracoes.encerrar(TipoConfiguracaoTemporal.PARAMETRO, identificador, requisicao);
  }

  @PatchMapping("/metas/{identificador}/vigencia")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void encerrarMeta(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoEncerramentoVigencia requisicao) {
    configuracoes.encerrar(TipoConfiguracaoTemporal.META, identificador, requisicao);
  }
}
