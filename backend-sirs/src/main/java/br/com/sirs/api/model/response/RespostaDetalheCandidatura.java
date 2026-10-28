package br.com.sirs.api.model.response;

import java.time.*;
import java.util.List;

public record RespostaDetalheCandidatura(
    RespostaCandidatura candidatura,
    RespostaCandidato candidato,
    List<RespostaAvaliacaoCriterio> notas,
    List<RespostaMovimentacaoCandidatura> historico,
    List<Long> avaliadores,
    RespostaProposta proposta,
    List<RespostaEtapaVaga> etapas,
    List<RespostaCriterioVaga> criterios) {}
