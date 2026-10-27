package br.com.sirs.api.controller;

import br.com.sirs.api.model.response.*;
import br.com.sirs.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Responsáveis por vagas",
    description =
        "Nomes e identificadores de responsáveis ativos, sem expor dados internos da conta.")
@RestController
@RequestMapping("/api/v1/cadastros")
@RequiredArgsConstructor
public class ResponsavelVagaController {
  private final UsuarioService usuarios;

  @Operation(
      summary = "Listar recrutadores disponíveis",
      operationId = "listarRecrutadoresDisponiveis")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/recrutadores")
  public RespostaPaginada<RespostaResponsavelVaga> listarRecrutadores(
      @ParameterObject Pageable paginacao) {
    return usuarios.listarResponsaveis(false, paginacao);
  }

  @Operation(
      summary = "Listar solicitantes disponíveis",
      operationId = "listarSolicitantesDisponiveis")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping("/solicitantes")
  public RespostaPaginada<RespostaResponsavelVaga> listarSolicitantes(
      @ParameterObject Pageable paginacao) {
    return usuarios.listarResponsaveis(true, paginacao);
  }
}
