package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoVisitasCanal;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.model.response.RespostaVisitasCanal;
import br.com.sirs.api.service.VisitasCanalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visitas-canais")
@RequiredArgsConstructor
public class VisitasCanalController {
  private final VisitasCanalService visitas;

  @GetMapping
  public RespostaPaginada<RespostaVisitasCanal> listar(Pageable paginacao) {
    return visitas.listar(paginacao);
  }

  @PutMapping
  public RespostaVisitasCanal registrar(@Valid @RequestBody RequisicaoVisitasCanal requisicao) {
    return visitas.registrar(requisicao);
  }
}
