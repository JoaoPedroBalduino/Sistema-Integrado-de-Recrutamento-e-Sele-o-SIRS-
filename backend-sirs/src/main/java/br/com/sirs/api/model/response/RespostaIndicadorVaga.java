package br.com.sirs.api.model.response;

import java.math.BigDecimal;
import java.util.*;

public record RespostaIndicadorVaga(
    Long vagaId,
    Long diasFechamento,
    BigDecimal candidatosPorPosicao,
    long aprovados,
    BigDecimal mediaNotaFinal,
    List<RespostaIndicadorEtapa> funil,
    List<RespostaIndicadorCriterio> criterios) {}
