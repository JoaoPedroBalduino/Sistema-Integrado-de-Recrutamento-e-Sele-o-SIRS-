package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.RequisicaoVisitasCanal;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.model.response.RespostaVisitasCanal;
import br.com.sirs.api.service.VisitasCanalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Visitas por canal",
    description = "Volumes mensais usados no cálculo da conversão por canal.")
@RestController
@RequestMapping("/api/v1/visitas-canais")
@RequiredArgsConstructor
public class VisitasCanalController {
  private final VisitasCanalService visitas;

  @Operation(summary = "Listar visitas dos canais", operationId = "listarVisitasDosCanais")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping
  public RespostaPaginada<RespostaVisitasCanal> listar(@ParameterObject Pageable paginacao) {
    return visitas.listar(paginacao);
  }

  @Operation(
      summary = "Registrar visitas do canal",
      operationId = "registrarVisitasDoCanal",
      description =
          "Exige COORD_RH ou RECRUTADOR. O período deve ser o primeiro dia do mês; o registro mensal existente é atualizado.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PutMapping
  public RespostaVisitasCanal registrar(@Valid @RequestBody RequisicaoVisitasCanal requisicao) {
    return visitas.registrar(requisicao);
  }
}
