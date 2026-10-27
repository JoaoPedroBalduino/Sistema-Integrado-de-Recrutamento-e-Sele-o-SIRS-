package br.com.sirs.api.model.response;

import java.time.*;
import java.util.*;

public record RespostaDetalheVaga(
    RespostaVaga vaga,
    List<RespostaEtapaVaga> etapas,
    List<RespostaCriterioVaga> criterios,
    List<RespostaHistoricoVaga> historico,
    List<RespostaApontamentoHoras> apontamentos) {}
