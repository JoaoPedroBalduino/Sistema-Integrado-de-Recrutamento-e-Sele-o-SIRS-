package br.com.sirs.api.model.response;

import java.math.BigDecimal;
import java.util.*;

public record RespostaIndicadorCriterio(Long criterioId, String nome, BigDecimal mediaNota) {}
