package br.com.sirs.api.model.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record RespostaPaginada<T>(
    List<T> content, int paginacao, int size, long totalElements, int totalPages) {
  public static <T> RespostaPaginada<T> of(Page<T> paginacao) {
    return new RespostaPaginada<>(
        paginacao.getContent(),
        paginacao.getNumber(),
        paginacao.getSize(),
        paginacao.getTotalElements(),
        paginacao.getTotalPages());
  }
}
