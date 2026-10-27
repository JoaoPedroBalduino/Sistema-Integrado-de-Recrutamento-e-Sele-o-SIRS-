package br.com.sirs.api.model.response;

import java.math.*;
import java.util.*;

public record RespostaEstimativaCusto(
    List<RespostaEtapaCusto> etapas,
    BigDecimal custoHoras,
    BigDecimal investimentosDiretos,
    BigDecimal total) {}
