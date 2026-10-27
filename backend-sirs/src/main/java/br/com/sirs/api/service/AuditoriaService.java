package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.model.response.RespostaAuditoria;
import br.com.sirs.api.model.response.RespostaPaginada;
import br.com.sirs.api.repository.AuditoriaRepository;
import br.com.sirs.api.security.ContextoAcesso;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaService {
  private final AuditoriaRepository auditoria;
  private final ContextoAcesso acesso;

  public RespostaPaginada<RespostaAuditoria> listar(Pageable paginacao) {
    acesso.exigirPerfil(ADMIN, COORD_RH);
    return RespostaPaginada.of(
        new PageImpl<>(
            auditoria.listar(paginacao.getPageSize(), paginacao.getOffset()),
            paginacao,
            auditoria.contar()));
  }
}
