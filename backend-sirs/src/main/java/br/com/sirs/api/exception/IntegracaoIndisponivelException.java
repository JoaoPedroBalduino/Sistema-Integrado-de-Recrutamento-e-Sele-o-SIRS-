package br.com.sirs.api.exception;

public class IntegracaoIndisponivelException extends RuntimeException {
  public IntegracaoIndisponivelException() {
    super("Servico de autenticacao temporariamente indisponivel.");
  }
}
