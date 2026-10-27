package br.com.sirs.api.model.response;

import java.math.BigDecimal;
import java.util.*;

public record RespostaIndicadorCargo(
    Long cargoId,
    String nome,
    long posicoesAbertas,
    long candidatos,
    BigDecimal mediaDiasAberto,
    BigDecimal investimentoMedio) {}
