package br.com.sirs.api.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entidade, Object id) {
        super(entidade + " nao encontrado(a): " + id);
    }
}
