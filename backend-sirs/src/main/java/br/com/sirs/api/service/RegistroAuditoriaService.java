package br.com.sirs.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistroAuditoriaService {
  private final JdbcTemplate bancoDados;

  /** No passwords, JWTs or candidate PII are copied to the audit trail. */
  public void registrarAuditoria(String entidade, Long identificador, String acao) {
    var autenticacao = SecurityContextHolder.getContext().getAuthentication();
    Long usuario =
        autenticacao != null && autenticacao.getName().matches("[0-9]+")
            ? Long.valueOf(autenticacao.getName())
            : null;
    bancoDados.update(
        "insert into auditoria(usuario_id,entidade,entidade_id,acao) values (?,?,?,?)",
        usuario,
        entidade,
        identificador,
        acao);
  }
}
