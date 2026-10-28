<script setup lang="ts">
import type { RespostaResumoIndicadores } from '#shared/types/api'
import { formatarNumero, formatarData } from '~/utils/formatacao'
import { limitarPercentual } from '~/utils/graficos'

const identificadorTitulo = useId()
const propriedades = defineProps<{ resumo: RespostaResumoIndicadores }>()
const percentual = computed(() => limitarPercentual(propriedades.resumo.percentualNoPrazo))
const temDados = computed(() => propriedades.resumo.vagasConsideradas > 0)
const meta = computed(() => propriedades.resumo.metasVigentes.VAGAS_NO_PRAZO)
</script>

<template>
  <section class="period-summary" :aria-labelledby="identificadorTitulo">
    <div class="period-summary-header">
      <span><SirsIcone nome="chart" :tamanho="15" />DESEMPENHO</span
      ><NuxtLink to="/indicadores" aria-label="Abrir indicadores do período"
        ><SirsIcone nome="external" :tamanho="17"
      /></NuxtLink>
    </div>
    <h2 :id="identificadorTitulo">Resultados do período</h2>
    <div v-if="temDados" class="period-summary-score">
      <strong>{{ formatarNumero(resumo.percentualNoPrazo, 1) }}<small>%</small></strong
      ><span>das vagas consideradas<br />fechadas no prazo</span>
    </div>
    <p v-else class="period-summary-empty">Sem vagas consideradas neste período.</p>
    <template v-if="temDados">
      <div
        class="period-summary-track"
        role="progressbar"
        aria-label="Percentual de vagas no prazo"
        :aria-valuenow="percentual"
        aria-valuemin="0"
        aria-valuemax="100"
      >
        <span :style="{ width: percentual + '%' }" />
      </div>
      <div class="period-summary-caption">
        <span>{{ formatarNumero(resumo.vagasConsideradas) }} vagas consideradas</span
        ><span v-if="meta != null">Meta {{ formatarNumero(meta, 1) }}%</span>
      </div>
    </template>
    <div class="period-summary-details">
      <div>
        <span>Ciclo de admissão</span
        ><strong
          >{{ temDados ? formatarNumero(resumo.mediaDiasAdmissao, 1) : '—' }}
          <small v-if="temDados">dias</small></strong
        >
      </div>
      <div>
        <span>Vagas atrasadas</span><strong>{{ formatarNumero(resumo.vagasAtrasadas) }}</strong>
      </div>
    </div>
    <p class="period-summary-range">
      {{ formatarData(resumo.de) }} a {{ formatarData(resumo.ate) }}
    </p>
  </section>
</template>

<style scoped>
.period-summary {
  padding: 24px;
  margin-bottom: 22px;
  border-radius: 14px;
  background: #292745;
  color: #fff;
  position: relative;
  overflow: hidden;
  box-shadow: 0 10px 24px #25233d10;
}
.period-summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}
.period-summary-header > span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #c4bcf2;
  font-size: 9px;
  letter-spacing: 1px;
  font-weight: 500;
}
.period-summary-header a {
  color: #d6d0f5;
  display: inline-flex;
  padding: 4px;
}
.period-summary-header a:hover {
  color: #fff;
}
h2 {
  color: #fff;
  font-size: 17px;
  font-weight: 550;
  letter-spacing: -0.3px;
}
.period-summary-score {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin: 23px 0 18px;
}
.period-summary-score > strong {
  font-size: 43px;
  line-height: 1;
  font-weight: 550;
  letter-spacing: -2px;
}
.period-summary-score small {
  font-size: 24px;
  font-weight: 400;
  margin-left: 3px;
  color: #b3a8ec;
}
.period-summary-score > span {
  font-size: 11px;
  line-height: 1.7;
  color: #cbc6df;
}
.period-summary-track {
  height: 6px;
  border-radius: 8px;
  background: #46405f;
  overflow: hidden;
}
.period-summary-track > span {
  display: block;
  height: 100%;
  background: #b6a7fa;
  border-radius: inherit;
}
.period-summary-caption {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  color: #c0b9d9;
  font-size: 10px;
  line-height: 1.6;
}
.period-summary-details {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #4c4562;
}
.period-summary-details > div > span {
  display: block;
  color: #cbc5df;
  font-size: 10px;
  margin-bottom: 8px;
}
.period-summary-details strong {
  color: #fff;
  font-size: 22px;
  font-weight: 500;
}
.period-summary-details small {
  color: #c3bbd9;
  font-size: 11px;
  font-weight: 400;
}
.period-summary-range {
  margin-top: 22px;
  color: #b6adce;
  font-size: 10px;
}
.period-summary-empty {
  margin-top: 22px;
  color: #d4cee3;
  font-size: 13px;
  line-height: 1.8;
}
</style>
