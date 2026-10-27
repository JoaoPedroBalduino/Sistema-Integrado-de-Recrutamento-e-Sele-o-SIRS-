package br.com.sirs.api.model.response;

import java.math.BigDecimal;
import java.util.*;

public record RespostaIndicadorCanal(
    Long canalId,
    String nome,
    long visitas,
    long contratacoes,
    BigDecimal percentualCanal,
    BigDecimal percentualGeral) {}
