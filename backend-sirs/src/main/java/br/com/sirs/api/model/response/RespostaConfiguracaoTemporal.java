package br.com.sirs.api.model.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.*;

public record RespostaConfiguracaoTemporal(
    Long identificador,
    String chave,
    BigDecimal valor,
    LocalDate vigenciaInicio,
    LocalDate vigenciaFim) {}
