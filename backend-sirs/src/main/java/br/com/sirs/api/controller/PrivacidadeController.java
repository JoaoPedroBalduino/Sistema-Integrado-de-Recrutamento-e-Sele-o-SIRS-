package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoSolicitacaoTitular;
import br.com.sirs.api.model.response.RespostaExportacaoTitular;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.model.response.RespostaSolicitacaoTitular;
import br.com.sirs.api.service.PrivacidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/privacidade")
@RequiredArgsConstructor
public class PrivacidadeController {
  private final PrivacidadeService privacidade;

  @GetMapping("/solicitacoes")
  public RespostaPaginada<RespostaSolicitacaoTitular> listar(Pageable paginacao) {
    return privacidade.listar(paginacao);
  }

  @PostMapping("/solicitacoes")
  @ResponseStatus(HttpStatus.CREATED)
  public RespostaSolicitacaoTitular cadastrar(
      @Valid @RequestBody RequisicaoSolicitacaoTitular requisicao) {
    return privacidade.cadastrar(requisicao);
  }

  @PostMapping("/solicitacoes/{identificador}/conclusao")
  public RespostaSolicitacaoTitular concluir(@PathVariable Long identificador) {
    return privacidade.concluir(identificador);
  }

  @GetMapping("/candidatos/{candidatoId}/exportacao")
  public RespostaExportacaoTitular exportar(@PathVariable Long candidatoId) {
    return privacidade.exportar(candidatoId);
  }
}
