package br.com.sirs.api.model.response;

import br.com.sirs.api.model.enums.StatusCandidatura;
import java.math.BigDecimal;
import java.time.*;

public record RespostaCandidatura(
    Long identificador,
    Long vagaId,
    String tituloVaga,
    Long candidatoId,
    String nomeCandidato,
    String emailCandidato,
    Long canalId,
    Long etapaAtualId,
    String etapaAtual,
    StatusCandidatura status,
    BigDecimal notaFinal,
    Instant contratadaEm) {}
