package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.*;
import br.com.sirs.api.model.response.*;
import br.com.sirs.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Usuários",
    description = "Administração das contas internas, perfis, bloqueio e ativação.")
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
  private final UsuarioService usuarios;

  @Operation(
      summary = "Listar usuários",
      operationId = "listarUsuarios",
      description = "Disponível para ADMIN e COORD_RH. Senhas e hashes nunca são retornados.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping
  public RespostaPaginada<RespostaUsuario> listar(@ParameterObject Pageable paginacao) {
    return usuarios.listarUsuarios(paginacao);
  }

  @Operation(
      summary = "Cadastrar usuário",
      operationId = "cadastrarUsuario",
      description =
          "Somente ADMIN. A senha deve conter pelo menos 12 caracteres e no máximo 72 bytes UTF-8.")
  @ApiResponse(
      responseCode = "201",
      description = "Recurso criado com sucesso.",
      useReturnTypeSchema = true)
  @PostMapping
  public ResponseEntity<RespostaUsuario> cadastrar(
      @Valid @RequestBody RequisicaoCadastroUsuario requisicao) {
    var resposta = usuarios.cadastrar(requisicao);
    return ResponseEntity.created(URI.create("/api/v1/usuarios/" + resposta.identificador()))
        .body(resposta);
  }

  @Operation(
      summary = "Atualizar acesso do usuário",
      operationId = "atualizarAcessoDoUsuario",
      description =
          "Somente ADMIN. Alterações revogam tokens anteriores. Um administrador não pode remover o próprio acesso.")
  @ApiResponse(
      responseCode = "200",
      description = "Operação concluída com sucesso.",
      useReturnTypeSchema = true)
  @PutMapping("/{identificador}")
  public RespostaUsuario atualizar(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoAtualizacaoUsuario requisicao) {
    return usuarios.atualizar(identificador, requisicao);
  }
}
