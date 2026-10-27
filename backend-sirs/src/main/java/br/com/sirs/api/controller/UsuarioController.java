package br.com.sirs.api.controller;

import br.com.sirs.api.model.request.*;
import br.com.sirs.api.model.request.RequisicaoVinculoSupabase;
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
    name = "UsuÃ¡rios",
    description = "AdministraÃ§Ã£o das contas internas, perfis, bloqueio e ativaÃ§Ã£o.")
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
  private final UsuarioService usuarios;

  @Operation(
      summary = "Listar usuÃ¡rios",
      operationId = "listarUsuarios",
      description = "DisponÃ­vel para ADMIN e COORD_RH. Senhas e hashes nunca sÃ£o retornados.")
  @ApiResponse(
      responseCode = "200",
      description = "OperaÃ§Ã£o concluÃ­da com sucesso.",
      useReturnTypeSchema = true)
  @GetMapping
  public RespostaPaginada<RespostaUsuario> listar(@ParameterObject Pageable paginacao) {
    return usuarios.listarUsuarios(paginacao);
  }

  @Operation(
      summary = "Cadastrar usuÃ¡rio",
      operationId = "cadastrarUsuario",
      description =
          "Somente ADMIN. A senha deve conter pelo menos 12 caracteres e no mÃ¡ximo 72 bytes UTF-8.")
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
      summary = "Atualizar acesso do usuÃ¡rio",
      operationId = "atualizarAcessoDoUsuario",
      description =
          "Somente ADMIN. AlteraÃ§Ãµes revogam tokens anteriores. Um administrador nÃ£o pode remover o prÃ³prio acesso.")
  @ApiResponse(
      responseCode = "200",
      description = "OperaÃ§Ã£o concluÃ­da com sucesso.",
      useReturnTypeSchema = true)
  @PutMapping("/{identificador}")
  public RespostaUsuario atualizar(
      @PathVariable Long identificador,
      @Valid @RequestBody RequisicaoAtualizacaoUsuario requisicao) {
    return usuarios.atualizar(identificador, requisicao);
  }

  @Operation(
      summary = "Vincular usuário ao Supabase",
      operationId = "vincularUsuarioSupabase",
      description =
          "Somente ADMIN. Vincula uma conta Supabase existente cujo e-mail coincide com o cadastro SIRS. Revoga sessões anteriores.")
  @ApiResponse(
      responseCode = "200",
      description = "Conta vinculada com sucesso.",
      useReturnTypeSchema = true)
  @PutMapping("/{identificador}/supabase")
  public RespostaUsuario vincularSupabase(
      @PathVariable Long identificador, @Valid @RequestBody RequisicaoVinculoSupabase requisicao) {
    return usuarios.vincularSupabase(identificador, requisicao.usuarioSupabaseId());
  }
}
