package br.com.sirs.api.exception;

/** Violacao de uma regra de negocio (RN-xx) ou de uma validacao de requisito (RF-xx). */
public class RegraNegocioException extends RuntimeException {

  public RegraNegocioException(String message) {
    super(message);
  }
}
