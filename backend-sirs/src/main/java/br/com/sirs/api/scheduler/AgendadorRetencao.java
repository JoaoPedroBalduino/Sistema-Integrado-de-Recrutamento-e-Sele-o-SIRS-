package br.com.sirs.api.scheduler;

import br.com.sirs.api.repository.CandidatoRepository;
import br.com.sirs.api.service.AnonimizacaoService;
import java.time.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AgendadorRetencao {
  private final CandidatoRepository candidatos;
  private final AnonimizacaoService anonimizacao;
  private final Clock relogio;

  @Scheduled(cron = "${sirs.privacy.cron}", zone = "America/Sao_Paulo")
  public void executarRetencao() {
    var limite = ZonedDateTime.now(relogio).minusMonths(12).toInstant();
    for (var identificador : candidatos.findElegiveisRetencao(limite, PageRequest.of(0, 500))) {
      try {
        anonimizacao.anonimizar(identificador, true);
      } catch (RuntimeException excecao) {
        log.error("Falha na rotina de retencao para identificador {}", identificador, excecao);
      }
    }
  }
}
