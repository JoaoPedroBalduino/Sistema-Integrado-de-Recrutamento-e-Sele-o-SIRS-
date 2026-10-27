package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.exception.RegraNegocioException;
import br.com.sirs.api.model.entity.VisitasCanal;
import br.com.sirs.api.model.request.RequisicaoVisitasCanal;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.model.response.RespostaVisitasCanal;
import br.com.sirs.api.repository.VisitasCanalRepository;
import br.com.sirs.api.security.ContextoAcesso;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitasCanalService {

  private final VisitasCanalRepository visitas;
  private final CadastroApoioService cadastros;
  private final ContextoAcesso acesso;
  private final RegistroAuditoriaService auditoria;

  public RespostaPaginada<RespostaVisitasCanal> listar(Pageable paginacao) {
    acesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, DIRETORIA);
    return RespostaPaginada.of(visitas.findAll(paginacao).map(RespostaVisitasCanal::from));
  }

  @Transactional
  public RespostaVisitasCanal registrar(RequisicaoVisitasCanal requisicao) {
    acesso.exigirPerfil(COORD_RH, RECRUTADOR);
    cadastros.canal(requisicao.canalId());
    if (requisicao.periodo().getDayOfMonth() != 1)
      throw new RegraNegocioException("Periodo deve ser o primeiro dia do mes.");
    var registro =
        visitas
            .findByCanalIdAndPeriodo(requisicao.canalId(), requisicao.periodo())
            .orElseGet(VisitasCanal::new);
    boolean novo = registro.getId() == null;
    registro.setCanalId(requisicao.canalId());
    registro.setPeriodo(requisicao.periodo());
    registro.setVisitas(requisicao.visitas());
    visitas.saveAndFlush(registro);
    auditoria.registrarAuditoria("visitas_canal", registro.getId(), novo ? "CRIACAO" : "ALTERACAO");
    return RespostaVisitasCanal.from(registro);
  }
}
