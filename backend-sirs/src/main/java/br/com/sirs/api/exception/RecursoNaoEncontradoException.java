package br.com.sirs.api.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

  public RecursoNaoEncontradoException(String entidade, Object identificador) {
    super(entidade + " nao encontrado(a): " + identificador);
  }
}
