package br.com.sirs.api.model.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public record RespostaResumoIndicadores(
    LocalDate de,
    LocalDate ate,
    long vagasFechadas,
    long vagasConsideradas,
    long vagasAtrasadas,
    BigDecimal horasPorVagaFechada,
    BigDecimal percentualNoPrazo,
    BigDecimal custoPorVagaFechada,
    BigDecimal mediaDiasAdmissao,
    Map<String, BigDecimal> metasVigentes) {}
