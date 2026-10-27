package br.com.sirs.api.controller;

import br.com.sirs.api.model.response.RespostaAuditoria;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {
  private final AuditoriaService auditoria;

  @GetMapping
  public RespostaPaginada<RespostaAuditoria> listar(Pageable paginacao) {
    return auditoria.listar(paginacao);
  }
}
