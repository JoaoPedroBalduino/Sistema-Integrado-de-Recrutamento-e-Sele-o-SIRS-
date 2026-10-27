package br.com.sirs.api.controller;

import br.com.sirs.api.model.response.RespostaAuditoria;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Auditoria",
    description = "Consulta paginada da trilha de ações. Disponível para ADMIN e COORD_RH.")
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {
  private final AuditoriaService auditoria;

  @Operation(summary = "Listar registros de auditoria", operationId = "listarRegistrosDeAuditoria")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping
  public RespostaPaginada<RespostaAuditoria> listar(@ParameterObject Pageable paginacao) {
    return auditoria.listar(paginacao);
  }
}
