package br.com.sirs.api.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class TratamentoExcecoes {
  @ExceptionHandler(ErrorResponseException.class)
  ProblemDetail tratarRespostaHttp(ErrorResponseException excecao) {
    return problem(HttpStatus.valueOf(excecao.getStatusCode().value()), "Requisicao nao atendida.");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ProblemDetail tratarRotaInexistente(Exception excecao) {
    return problem(HttpStatus.NOT_FOUND, "Recurso nao encontrado.");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ProblemDetail tratarContratoHttp(Exception excecao) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Confira os parametros e o metodo HTTP.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ProblemDetail tratarMetodoInvalido(Exception excecao) {
    return problem(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP nao permitido.");
  }

  private ProblemDetail problem(HttpStatus status, String detalhe) {
    var problema = ProblemDetail.forStatusAndDetail(status, detalhe);
    problema.setProperty("requestId", MDC.get("requestId"));
    return problema;
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  ProblemDetail notFound(RecursoNaoEncontradoException excecao) {
    return problem(HttpStatus.NOT_FOUND, excecao.getMessage());
  }

  @ExceptionHandler(RegraNegocioException.class)
  ProblemDetail business(RegraNegocioException excecao) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, excecao.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(MethodArgumentNotValidException excecao) {
    var problema = problem(HttpStatus.BAD_REQUEST, "Dados invalidos.");
    var campos = new TreeMap<String, String>();
    excecao
        .getBindingResult()
        .getFieldErrors()
        .forEach(
            erroCampo -> campos.putIfAbsent(erroCampo.getField(), erroCampo.getDefaultMessage()));
    problema.setProperty("errors", campos);
    return problema;
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    ConstraintViolationException.class
  })
  ProblemDetail malformed(Exception excecao) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Requisicao invalida: confira os tipos, campos e valores informados.");
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    PessimisticLockingFailureException.class
  })
  ProblemDetail conflict(Exception excecao) {
    return problem(
        HttpStatus.CONFLICT,
        "Conflito de dados ou alteracao concorrente. Atualize os dados antes de repetir.");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail forbidden(AccessDeniedException excecao) {
    return problem(HttpStatus.FORBIDDEN, "Acesso negado.");
  }

  @ExceptionHandler(BadCredentialsException.class)
  ProblemDetail unauthorized(BadCredentialsException excecao) {
    return problem(HttpStatus.UNAUTHORIZED, "Credenciais invalidas ou conta indisponivel.");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception excecao) {
    log.error("Falha inesperada no processamento da requisicao", excecao);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Informe o requestId ao suporte.");
  }
}
