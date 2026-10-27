package br.com.sirs.api.service;

import static br.com.sirs.api.model.enums.ChaveParametroCusto.*;

import br.com.sirs.api.model.enums.ChaveParametroCusto;
import br.com.sirs.api.model.response.RespostaEstimativaCusto;
import br.com.sirs.api.model.response.RespostaEtapaCusto;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraCusto {

  public RespostaEstimativaCusto calcular(
      Map<ChaveParametroCusto, BigDecimal> parametros, BigDecimal investimentoDivulgacao) {
    Map<ChaveParametroCusto, BigDecimal> valores = new EnumMap<>(ChaveParametroCusto.class);
    for (var chave : ChaveParametroCusto.values())
      valores.put(chave, parametros.getOrDefault(chave, chave.valorPadrao()));
    valores.forEach(ChaveParametroCusto::validar);
    var horaGestor = valorHora(valores, SALARIO_GESTOR);
    var horaRecrutador = valorHora(valores, SALARIO_RECRUTADOR);
    var horaColaborador = valorHora(valores, SALARIO_COLABORADOR);
    var horaDesigner = valorHora(valores, SALARIO_DESIGNER);
    List<RespostaEtapaCusto> etapas = new ArrayList<>();
    etapas.add(
        new RespostaEtapaCusto(
            "Briefing",
            valores.get(HORAS_BRIEFING_GESTOR).add(valores.get(HORAS_BRIEFING_RECRUTADOR)),
            valores
                .get(HORAS_BRIEFING_GESTOR)
                .multiply(horaGestor)
                .add(valores.get(HORAS_BRIEFING_RECRUTADOR).multiply(horaRecrutador))));
    adicionar(etapas, "Cadastro da vaga", valores.get(HORAS_CADASTRO), horaRecrutador);
    adicionar(etapas, "Criativo de divulgacao", valores.get(HORAS_CRIATIVO), horaDesigner);
    adicionar(
        etapas,
        "Triagem",
        valores
            .get(CURRICULOS_TRIADOS)
            .multiply(valores.get(MINUTOS_TRIAGEM))
            .divide(new BigDecimal("60"), 12, RoundingMode.HALF_UP),
        horaRecrutador);
    adicionar(
        etapas,
        "Dinamica",
        valores
            .get(HORAS_DINAMICA)
            .multiply(valores.get(PARTICIPANTES_DINAMICA))
            .add(valores.get(HORAS_PREPARACAO_DINAMICA)),
        horaRecrutador);
    adicionar(
        etapas,
        "Desafio",
        valores.get(HORAS_DESAFIO).multiply(valores.get(CANDIDATOS_DESAFIO)),
        horaRecrutador);
    var horasEntrevista =
        valores.get(HORAS_ENTREVISTA).multiply(valores.get(CANDIDATOS_ENTREVISTA));
    etapas.add(
        new RespostaEtapaCusto(
            "Entrevista com gestor",
            horasEntrevista.multiply(new BigDecimal("2")),
            horasEntrevista.multiply(horaGestor.add(horaRecrutador))));
    adicionar(
        etapas,
        "Fit cultural",
        valores
            .get(CANDIDATOS_FIT_CULTURAL)
            .multiply(valores.get(COLABORADORES_FIT_CULTURAL))
            .multiply(valores.get(HORAS_FIT_CULTURAL)),
        horaColaborador);
    adicionar(etapas, "Proposta", valores.get(HORAS_PROPOSTA), horaRecrutador);
    adicionar(etapas, "Orientacoes", valores.get(HORAS_ORIENTACOES), horaRecrutador);
    var custoHoras =
        etapas.stream()
            .map(RespostaEtapaCusto::custo)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    var diretos =
        valores
            .get(KIT_BOAS_VINDAS)
            .add(
                investimentoDivulgacao == null
                    ? valores.get(VERBA_DIVULGACAO)
                    : investimentoDivulgacao)
            .setScale(2, RoundingMode.HALF_UP);
    // PA-07: usa divulgacao, nao OUTROS_INVESTIMENTOS; arredonda somente a apresentacao.
    return new RespostaEstimativaCusto(
        etapas.stream()
            .map(
                etapa ->
                    new RespostaEtapaCusto(
                        etapa.descricao(),
                        etapa.horas(),
                        etapa.custo().setScale(2, RoundingMode.HALF_UP)))
            .toList(),
        custoHoras,
        diretos,
        custoHoras.add(diretos));
  }

  private BigDecimal valorHora(
      Map<ChaveParametroCusto, BigDecimal> valores, ChaveParametroCusto salario) {
    return valores
        .get(salario)
        .multiply(valores.get(FATOR_ENCARGOS))
        .divide(valores.get(HORAS_MENSAIS), 12, RoundingMode.HALF_UP);
  }

  private void adicionar(
      List<RespostaEtapaCusto> etapas, String descricao, BigDecimal horas, BigDecimal valorHora) {
    etapas.add(new RespostaEtapaCusto(descricao, horas, horas.multiply(valorHora)));
  }
}
