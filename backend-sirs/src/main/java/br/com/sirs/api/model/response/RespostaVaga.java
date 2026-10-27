package br.com.sirs.api.model.response;

import br.com.sirs.api.model.enums.StatusVaga;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record RespostaVaga(
    Long identificador,
    long version,
    String titulo,
    RespostaCadastroApoio cargo,
    RespostaCadastroApoio area,
    Long solicitanteId,
    Long recrutadorId,
    int posicoes,
    String cidade,
    String estado,
    boolean remoto,
    String observacoes,
    StatusVaga status,
    LocalDate dataAbertura,
    LocalDate dataPublicacao,
    LocalDate dataFechamento,
    Integer prazoAlvoDias,
    BigDecimal investimentoDivulgacao,
    List<RespostaCadastroApoio> canais,
    long candidatos,
    long posicoesPreenchidas,
    Long diasAberto) {}
