package br.com.sirs.api.model;

import static org.junit.jupiter.api.Assertions.*;

import br.com.sirs.api.model.enums.StatusVaga;
import java.util.*;
import org.junit.jupiter.api.Test;

class StatusVagaTest {
  @Test
  void verificaTodasAsCombinacoesDeTransicoes() {
    var permitidas =
        Set.of(
            "RASCUNHO:EM_APROVACAO",
            "EM_APROVACAO:PUBLICADA",
            "EM_APROVACAO:RASCUNHO",
            "PUBLICADA:CONGELADA",
            "PUBLICADA:FECHADA",
            "CONGELADA:PUBLICADA",
            "CONGELADA:FECHADA");
    for (var origem : StatusVaga.values())
      for (var destino : StatusVaga.values())
        assertEquals(
            permitidas.contains(origem.name() + ":" + destino.name()),
            origem.permits(destino),
            origem + " -> " + destino);
  }
}
