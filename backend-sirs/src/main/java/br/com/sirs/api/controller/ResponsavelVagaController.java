package br.com.sirs.api.controller;

import br.com.sirs.api.model.response.*;
import br.com.sirs.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cadastros")
@RequiredArgsConstructor
public class ResponsavelVagaController {
  private final UsuarioService usuarios;

  @GetMapping("/recrutadores")
  public RespostaPaginada<RespostaResponsavelVaga> listarRecrutadores(Pageable paginacao) {
    return usuarios.listarResponsaveis(false, paginacao);
  }

  @GetMapping("/solicitantes")
  public RespostaPaginada<RespostaResponsavelVaga> listarSolicitantes(Pageable paginacao) {
    return usuarios.listarResponsaveis(true, paginacao);
  }
}
