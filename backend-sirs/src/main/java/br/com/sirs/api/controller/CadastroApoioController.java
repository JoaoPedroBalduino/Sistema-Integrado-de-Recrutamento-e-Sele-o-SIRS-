package br.com.sirs.api.controller;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.model.enums.TipoCadastroApoio;
import br.com.sirs.api.model.request.RequisicaoCadastroApoio;
import br.com.sirs.api.model.response.RespostaCadastroApoio;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.service.CadastroApoioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Cadastros de apoio",
    description =
        "Áreas, cargos e canais. Alterações exigem ADMIN ou COORD_RH. Tipos válidos: areas, cargos e canais.")
@RestController
@RequestMapping("/api/v1/cadastros/{tipo}")
@RequiredArgsConstructor
public class CadastroApoioController {
  private final CadastroApoioService cadastros;

  private TipoCadastroApoio tipo(String tipo) {
    try {
      return TipoCadastroApoio.valueOf(tipo.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException excecao) {
      throw new RecursoNaoEncontradoException("Cadastro", tipo);
    }
  }

  @Operation(summary = "Listar áreas cargos ou canais", operationId = "listarAreasCargosOuCanais")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping
  public RespostaPaginada<RespostaCadastroApoio> list(
      @PathVariable String tipo, @ParameterObject Pageable paginacao) {
    return cadastros.list(tipo(tipo), paginacao);
  }

  @Operation(summary = "Cadastrar área cargo ou canal", operationId = "cadastrarAreaCargoOuCanal")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping
  public ResponseEntity<RespostaCadastroApoio> create(
      @PathVariable String tipo, @Valid @RequestBody RequisicaoCadastroApoio requisicao) {
    var cadastro = cadastros.save(tipo(tipo), null, requisicao);
    return ResponseEntity.created(
            URI.create("/api/v1/cadastros/" + tipo + "/" + cadastro.identificador()))
        .body(cadastro);
  }

  @Operation(
      summary = "Atualizar área cargo ou canal",
      operationId = "atualizarAreaCargoOuCanal",
      description = "Use ativo=false para desativar o registro sem apagar referências históricas.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PutMapping("/{identificador}")
  public RespostaCadastroApoio update(
      @PathVariable String tipo,
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoCadastroApoio requisicao) {
    return cadastros.save(tipo(tipo), identificador, requisicao);
  }
}
