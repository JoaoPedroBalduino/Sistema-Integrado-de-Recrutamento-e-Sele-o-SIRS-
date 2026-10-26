package br.com.sirs.api.common.exception;

/**
 * Violacao de uma regra de negocio (RN-xx) ou de uma validacao de requisito (RF-xx).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
