package br.com.sirs.api.security;

import static br.com.sirs.api.model.enums.Perfil.*;

import br.com.sirs.api.model.entity.Vaga;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissaoVaga {
  private final ContextoAcesso contextoAcesso;

  public void verificarConsulta(Vaga vaga) {
    contextoAcesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, GESTOR, DIRETORIA);
    if (contextoAcesso.possuiPerfil(GESTOR)
        && !vaga.getSolicitante().getId().equals(contextoAcesso.identificador())) deny();
  }

  public void verificarAlteracao(Vaga vaga) {
    contextoAcesso.exigirPerfil(COORD_RH, RECRUTADOR, GESTOR);
    if (contextoAcesso.possuiPerfil(GESTOR)
        && !vaga.getSolicitante().getId().equals(contextoAcesso.identificador())) deny();
    if (contextoAcesso.possuiPerfil(RECRUTADOR)
        && !vaga.getRecrutador().getId().equals(contextoAcesso.identificador())) deny();
  }

  public void verificarConsultaSelecao(Vaga vaga) {
    contextoAcesso.exigirPerfil(ADMIN, COORD_RH, RECRUTADOR, GESTOR);
    verificarConsulta(vaga);
  }

  private void deny() {
    throw new AccessDeniedException("Vaga de outro responsavel");
  }
}
