package br.com.sirs.api.model.response;

import java.math.BigDecimal;
import java.util.*;

public record RespostaIndicadorRecrutador(
    Long usuarioId,
    String nome,
    long posicoesTrabalhadas,
    long posicoesPreenchidas,
    BigDecimal percentualFechamento) {}
