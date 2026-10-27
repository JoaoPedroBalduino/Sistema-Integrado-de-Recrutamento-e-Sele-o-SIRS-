package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.exception.*;
import br.com.sirs.api.model.entity.Usuario;
import br.com.sirs.api.model.request.*;
import br.com.sirs.api.model.response.*;
import br.com.sirs.api.repository.SessaoSupabaseRepository;
import br.com.sirs.api.repository.UsuarioRepository;
import br.com.sirs.api.security.ContextoAcesso;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {
  @Transactional(readOnly = true)
  public RespostaPaginada<RespostaResponsavelVaga> listarResponsaveis(
      boolean solicitantes, Pageable paginacao) {
    contextoAcesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, GESTOR, DIRETORIA);
    var perfis = solicitantes ? List.of(GESTOR, COORD_RH) : List.of(RECRUTADOR, COORD_RH);
    return RespostaPaginada.of(
        usuarios
            .findByPerfilInAndAtivoTrueAndBloqueadoFalse(perfis, paginacao)
            .map(usuario -> new RespostaResponsavelVaga(usuario.getId(), usuario.getNome())));
  }

  private final UsuarioRepository usuarios;
  private final PasswordEncoder codificadorSenha;
  private final ContextoAcesso contextoAcesso;
  private final RegistroAuditoriaService registroAuditoria;
  private final ObjectProvider<AutenticacaoSupabaseService> supabase;
  private final SessaoSupabaseRepository sessoesSupabase;
  private final Clock relogio;

  @Transactional(readOnly = true)
  public RespostaPaginada<RespostaUsuario> listarUsuarios(Pageable paginacao) {
    contextoAcesso.exigirPerfil(ADMIN, COORD_RH);
    return RespostaPaginada.of(usuarios.findAll(paginacao).map(RespostaUsuario::of));
  }

  @Transactional
  public RespostaUsuario cadastrar(RequisicaoCadastroUsuario requisicao) {
    contextoAcesso.exigirPerfil(ADMIN);
    var usuario = cadastrarInternamente(requisicao);
    registroAuditoria.registrarAuditoria("usuario", usuario.getId(), "CRIACAO");
    return RespostaUsuario.of(usuario);
  }

  @Transactional
  public void criarAdministradorInicial(String email, String senha) {
    if (!usuarios.existsByPerfil(ADMIN)) {
      cadastrarInternamente(new RequisicaoCadastroUsuario("Administrador", email, senha, ADMIN));
      return;
    }
    var externo = supabase.getIfAvailable();
    if (externo != null)
      usuarios
          .findByEmail(AutenticacaoService.normalizarEmail(email))
          .filter(usuario -> usuario.getPerfil() == ADMIN && usuario.getSupabaseUsuarioId() == null)
          .ifPresent(
              usuario -> {
                usuario.setSupabaseUsuarioId(externo.provisionarConta(usuario.getEmail(), senha));
                usuario.setSenhaHash(codificadorSenha.encode(UUID.randomUUID().toString()));
              });
  }

  private Usuario cadastrarInternamente(RequisicaoCadastroUsuario requisicao) {
    var email = AutenticacaoService.normalizarEmail(requisicao.email());
    if (usuarios.findByEmail(email).isPresent())
      throw new RegraNegocioException("E-mail ja cadastrado.");
    if (requisicao.senha().length() < 12
        || requisicao.senha().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new RegraNegocioException(
          "Senha deve ter ao menos 12 caracteres e no maximo 72 bytes UTF-8.");
    var usuario = new Usuario();
    usuario.setNome(requisicao.nome().strip());
    usuario.setEmail(email);
    var externo = supabase.getIfAvailable();
    if (externo != null) {
      usuario.setSupabaseUsuarioId(externo.provisionarConta(email, requisicao.senha()));
      usuario.setSenhaHash(codificadorSenha.encode(UUID.randomUUID().toString()));
    } else usuario.setSenhaHash(codificadorSenha.encode(requisicao.senha()));
    usuario.setPerfil(requisicao.perfil());
    return usuarios.saveAndFlush(usuario);
  }

  @Transactional
  public RespostaUsuario atualizar(Long identificador, RequisicaoAtualizacaoUsuario requisicao) {
    contextoAcesso.exigirPerfil(ADMIN);
    var usuario =
        usuarios
            .lockById(identificador)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", identificador));
    if (identificador.equals(contextoAcesso.identificador())
        && (!requisicao.ativo() || requisicao.bloqueado() || requisicao.perfil() != ADMIN))
      throw new RegraNegocioException("O administrador nao pode remover o proprio acesso.");
    usuario.setNome(requisicao.nome().strip());
    usuario.setPerfil(requisicao.perfil());
    usuario.setAtivo(requisicao.ativo());
    usuario.setBloqueado(requisicao.bloqueado());
    if (!requisicao.bloqueado()) usuario.setTentativasFalhas(0);
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    sessoesSupabase.revogarTodas(usuario.getId(), relogio.instant());
    registroAuditoria.registrarAuditoria("usuario", identificador, "ALTERACAO");
    return RespostaUsuario.of(usuario);
  }

  @Transactional
  public RespostaUsuario vincularSupabase(Long identificador, UUID usuarioSupabaseId) {
    contextoAcesso.exigirPerfil(ADMIN);
    var externo = supabase.getIfAvailable();
    if (externo == null)
      throw new RegraNegocioException("Vinculo disponivel somente com Supabase Auth.");
    var usuario =
        usuarios
            .lockById(identificador)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", identificador));
    externo.validarVinculo(usuarioSupabaseId, usuario.getEmail());
    usuario.setSupabaseUsuarioId(usuarioSupabaseId);
    usuario.setSenhaHash(codificadorSenha.encode(UUID.randomUUID().toString()));
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    sessoesSupabase.revogarTodas(usuario.getId(), relogio.instant());
    registroAuditoria.registrarAuditoria("usuario", identificador, "ALTERACAO");
    return RespostaUsuario.of(usuario);
  }
}
