package br.com.sirs.api.model.response;

import br.com.sirs.api.model.enums.StatusCandidatura;
import java.time.*;

public record RespostaInscricaoTitular(
    Long candidaturaId, Long vagaId, StatusCandidatura status, Instant contratadaEm) {}
