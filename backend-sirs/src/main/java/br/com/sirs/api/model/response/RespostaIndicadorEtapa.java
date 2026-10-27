package br.com.sirs.api.model.response;

import java.math.BigDecimal;
import java.util.*;

public record RespostaIndicadorEtapa(
    Long etapaId,
    String nome,
    long quantidadeAtual,
    long passaram,
    BigDecimal percentualConversao) {}
