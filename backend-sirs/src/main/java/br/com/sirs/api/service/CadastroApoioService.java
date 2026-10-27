package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.exception.RecursoNaoEncontradoException;
import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.entity.Area;
import br.com.sirs.api.model.entity.CadastroApoio;
import br.com.sirs.api.model.entity.Canal;
import br.com.sirs.api.model.entity.Cargo;
import br.com.sirs.api.model.enums.TipoCadastroApoio;
import br.com.sirs.api.model.request.RequisicaoCadastroApoio;
import br.com.sirs.api.model.response.RespostaCadastroApoio;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.repository.AreaRepository;
import br.com.sirs.api.repository.CadastroApoioRepository;
import br.com.sirs.api.repository.CanalRepository;
import br.com.sirs.api.repository.CargoRepository;
import br.com.sirs.api.security.ContextoAcesso;
import jakarta.validation.constraints.*;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CadastroApoioService {
  private final AreaRepository areas;
  private final CargoRepository cargos;
  private final CanalRepository canais;
  private final ContextoAcesso contextoAcesso;
  private final RegistroAuditoriaService registroAuditoria;

  public RespostaPaginada<RespostaCadastroApoio> list(TipoCadastroApoio tipo, Pageable paginacao) {
    return switch (tipo) {
      case AREAS -> RespostaPaginada.of(areas.findAll(paginacao).map(RespostaCadastroApoio::of));
      case CARGOS -> RespostaPaginada.of(cargos.findAll(paginacao).map(RespostaCadastroApoio::of));
      case CANAIS -> RespostaPaginada.of(canais.findAll(paginacao).map(RespostaCadastroApoio::of));
    };
  }

  @Transactional
  public RespostaCadastroApoio save(
      TipoCadastroApoio tipo, Long identificador, RequisicaoCadastroApoio requisicao) {
    contextoAcesso.exigirPerfil(ADMIN, COORD_RH);
    return switch (tipo) {
      case AREAS -> persist(areas, Area::new, identificador, requisicao, "area");
      case CARGOS -> persist(cargos, Cargo::new, identificador, requisicao, "cargo");
      case CANAIS -> persist(canais, Canal::new, identificador, requisicao, "canal");
    };
  }

  private <T extends CadastroApoio> RespostaCadastroApoio persist(
      CadastroApoioRepository<T> repositorio,
      Supplier<T> fabrica,
      Long identificador,
      RequisicaoCadastroApoio requisicao,
      String nome) {
    var cadastro =
        identificador == null
            ? fabrica.get()
            : repositorio
                .findById(identificador)
                .orElseThrow(() -> new RecursoNaoEncontradoException(nome, identificador));
    if (repositorio.existsByNomeIgnoreCaseAndIdNot(
        requisicao.nome().strip(), identificador == null ? -1L : identificador))
      throw new RegraNegocioException("Nome ja cadastrado.");
    cadastro.setNome(requisicao.nome().strip());
    cadastro.setAtivo(requisicao.ativo());
    repositorio.saveAndFlush(cadastro);
    registroAuditoria.registrarAuditoria(
        nome, cadastro.getId(), identificador == null ? "CRIACAO" : "ALTERACAO");
    return RespostaCadastroApoio.of(cadastro);
  }

  public Area area(Long identificador) {
    return active(areas, identificador, "Area");
  }

  public Cargo cargo(Long identificador) {
    return active(cargos, identificador, "Cargo");
  }

  public Canal canal(Long identificador) {
    return active(canais, identificador, "Canal");
  }

  private <T extends CadastroApoio> T active(
      CadastroApoioRepository<T> repositorio, Long identificador, String nome) {
    var cadastro =
        repositorio
            .findById(identificador)
            .orElseThrow(() -> new RecursoNaoEncontradoException(nome, identificador));
    if (!cadastro.isAtivo()) throw new RegraNegocioException(nome + " inativo(a).");
    return cadastro;
  }
}
