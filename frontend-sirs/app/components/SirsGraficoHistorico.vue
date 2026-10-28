<script setup lang="ts">
import { criarEscalaGrafico } from '~/utils/graficos'
import { formatarNumero } from '~/utils/formatacao'

const propriedades = defineProps<{
  periodos: ReadonlyArray<{ de: string; vagasFechadas: number }>
  titulo: string
}>()
const escala = computed(() =>
  criarEscalaGrafico(propriedades.periodos.map((periodo) => periodo.vagasFechadas)),
)
const largura = computed(() => Math.max(580, propriedades.periodos.length * 48))
const espaco = computed(() => (largura.value - 66) / Math.max(1, propriedades.periodos.length))
const altura = (valor: number) => (Math.max(0, valor) / escala.value.limite) * 130
function mes(data: string) {
  return new Intl.DateTimeFormat('pt-BR', { month: 'short', year: '2-digit' })
    .format(new Date(data + 'T12:00:00Z'))
    .replace('.', '')
}
</script>

<template>
  <div v-if="periodos.length" class="history-visual">
    <div class="history-scroll">
      <svg :viewBox="`0 0 ${largura} 198`" role="img" :aria-label="titulo">
        <g v-for="marcacao in escala.marcacoes" :key="marcacao">
          <line
            x1="42"
            :x2="largura - 12"
            :y1="158 - altura(marcacao)"
            :y2="158 - altura(marcacao)"
          />
          <text x="30" :y="162 - altura(marcacao)" text-anchor="end" class="axis-label">
            {{ formatarNumero(marcacao) }}
          </text>
        </g>
        <g v-for="(periodo, indice) in periodos" :key="periodo.de">
          <rect
            :x="42 + indice * espaco + (espaco - Math.min(34, espaco * 0.6)) / 2"
            :y="158 - altura(periodo.vagasFechadas)"
            :width="Math.min(34, espaco * 0.6)"
            :height="altura(periodo.vagasFechadas)"
            rx="5"
            :class="{ 'latest-period': indice === periodos.length - 1 }"
          >
            <title>
              {{ mes(periodo.de) }}: {{ formatarNumero(periodo.vagasFechadas) }} vagas fechadas
            </title>
          </rect>
          <text
            :x="42 + indice * espaco + espaco / 2"
            :y="149 - altura(periodo.vagasFechadas)"
            text-anchor="middle"
            class="bar-value"
          >
            {{ formatarNumero(periodo.vagasFechadas) }}
          </text>
          <text
            :x="42 + indice * espaco + espaco / 2"
            y="183"
            text-anchor="middle"
            class="axis-label"
          >
            {{ mes(periodo.de) }}
          </text>
        </g>
      </svg>
    </div>
    <div class="sr-only">
      <table>
        <caption>
          {{
            titulo
          }}
        </caption>
        <thead>
          <tr>
            <th scope="col">Mês</th>
            <th scope="col">Vagas fechadas</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="periodo in periodos" :key="periodo.de">
            <th scope="row">{{ mes(periodo.de) }}</th>
            <td>{{ periodo.vagasFechadas }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
  <p v-else class="history-empty">O histórico aparecerá quando houver períodos disponíveis.</p>
</template>

<style scoped>
.history-visual {
  position: relative;
}
.history-visual > .sr-only {
  top: 0;
  left: 0;
}
.history-scroll {
  overflow-x: auto;
}
svg {
  display: block;
  width: 100%;
  min-width: 440px;
}
line {
  stroke: #e8ebf3;
  stroke-dasharray: 3 4;
}
rect {
  fill: #c7c4ef;
}
rect.latest-period {
  fill: #6253cf;
}
rect:hover {
  fill: #8274e1;
}
.axis-label {
  fill: #768098;
  font-size: 10px;
  font-family: inherit;
}
.bar-value {
  fill: #4a5068;
  font-size: 11px;
  font-weight: 600;
  font-family: inherit;
}
.history-empty {
  padding: 24px 0;
  color: var(--muted);
  font-size: 13px;
}
</style>
