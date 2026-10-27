package br.com.sirs.api.model.response;

import java.time.*;
import java.util.List;

public record RespostaExportacaoTitular(
    RespostaCandidato candidato, List<RespostaInscricaoTitular> candidaturas) {}
