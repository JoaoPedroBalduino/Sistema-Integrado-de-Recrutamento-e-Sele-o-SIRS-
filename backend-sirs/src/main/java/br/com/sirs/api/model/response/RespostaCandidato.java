package br.com.sirs.api.model.response;

import br.com.sirs.api.model.entity.Candidato;
import br.com.sirs.api.model.enums.BaseLegal;
import java.time.Instant;

public record RespostaCandidato(
    Long identificador,
    String nome,
    String email,
    String curriculoUrl,
    BaseLegal baseLegal,
    Instant dataColeta,
    String versaoConsentimento,
    Instant anonimizadoEm) {
  public static RespostaCandidato from(Candidato candidato) {
    return new RespostaCandidato(
        candidato.getId(),
        candidato.getNome(),
        candidato.getEmail(),
        candidato.getCurriculoUrl(),
        candidato.getBaseLegal(),
        candidato.getConsentimentoEm(),
        candidato.getConsentimentoVersao(),
        candidato.getAnonimizadoEm());
  }
}
