package br.com.sirs.api;

import static org.junit.jupiter.api.Assertions.*;

import br.com.sirs.api.controller.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArquiteturaMvcTest {
  @Test
  void controllersNaoAcessamPersistenciaDiretamente() {
    for (var controlador :
        List.of(
            AutenticacaoController.class,
            UsuarioController.class,
            CadastroApoioController.class,
            VagaController.class,
            CandidatoController.class,
            CandidaturaController.class,
            CustoController.class,
            ConfiguracaoTemporalController.class,
            IndicadorController.class,
            VisitasCanalController.class,
            PrivacidadeController.class,
            AuditoriaController.class,
            ResponsavelVagaController.class)) {
      for (var dependencia : controlador.getDeclaredFields())
        assertFalse(
            dependencia.getType().getPackageName().contains("repository"),
            controlador.getSimpleName() + " depende de persistencia");
    }
  }
}
