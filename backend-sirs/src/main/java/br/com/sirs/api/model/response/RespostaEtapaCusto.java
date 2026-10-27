package br.com.sirs.api.model.response;

import java.math.*;
import java.util.*;

public record RespostaEtapaCusto(String descricao, BigDecimal horas, BigDecimal custo) {}
