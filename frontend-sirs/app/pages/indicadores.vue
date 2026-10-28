<script setup lang="ts">
import type {
  RespostaResumoIndicadores,
  RespostaIndicadorCargo,
  RespostaIndicadorRecrutador,
  RespostaIndicadorCanal,
} from '#shared/types/api'
import { perfisOperacionais } from '~/utils/permissoes'
import { formatarNumero, formatarMoeda, formatarData } from '~/utils/formatacao'
import { gerarCsv, baixarArquivo } from '~/utils/exportacao'
definePageMeta({ perfis: perfisOperacionais })
useHead({ title: 'Indicadores' })
const autenticacao = useAutenticacao()
const consulta = useApiSirs()
const hoje = dataHoje()
const de = ref(hoje.slice(0, 4) + '-01-01')
const ate = ref(hoje)
const periodo = ref({ de: de.value, ate: ate.value })
const aviso = ref('')
const aba = ref('cargos')
function aplicar() {
  const meses =
    (Number(ate.value.slice(0, 4)) - Number(de.value.slice(0, 4))) * 12 +
    Number(ate.value.slice(5, 7)) -
    Number(de.value.slice(5, 7))
  if (!de.value || !ate.value || de.value > ate.value || meses > 35) {
    aviso.value = 'Escolha um período válido de até 36 meses.'
    return
  }
  aviso.value = ''
  periodo.value = { de: de.value, ate: ate.value }
}
const { data, status, error, refresh } = await useAsyncData(
  'indicadores-painel',
  async () => {
    const [resumo, historico, cargos, recrutadores, canais] = await Promise.all([
      consulta<RespostaResumoIndicadores>('indicadores/resumo', { query: periodo.value }),
      consulta<RespostaResumoIndicadores[]>('indicadores/historico', { query: periodo.value }),
      consulta<RespostaIndicadorCargo[]>('indicadores/cargos', { query: periodo.value }),
      consulta<RespostaIndicadorRecrutador[]>('indicadores/recrutadores', { query: periodo.value }),
      autenticacao.usuario.value?.perfil === 'GESTOR'
        ? Promise.resolve([] as RespostaIndicadorCanal[])
        : consulta<RespostaIndicadorCanal[]>('indicadores/canais', { query: periodo.value }),
    ])
    return { resumo, historico, cargos, recrutadores, canais }
  },
  { watch: [periodo] },
)
const maiorVolume = computed(() =>
  Math.max(1, ...(data.value?.historico.map((mes) => mes.vagasFechadas) || [])),
)
function exportar() {
  if (!data.value) return
  baixarArquivo(
    'indicadores-sirs.csv',
    '\uFEFF' +
      gerarCsv(
        [
          'Início',
          'Fim',
          'Vagas fechadas',
          'Vagas atrasadas',
          'Percentual no prazo',
          'Custo por vaga',
          'Média de dias para admissão',
        ],
        data.value.historico.map((mes) => [
          mes.de,
          mes.ate,
          mes.vagasFechadas,
          mes.vagasAtrasadas,
          mes.percentualNoPrazo,
          mes.custoPorVagaFechada,
          mes.mediaDiasAdmissao,
        ]),
      ),
  )
}
</script>
<template>
  <SirsCabecalho
    titulo="Indicadores"
    descricao="Consulte os resultados do recrutamento por período."
    categoria="INDICADORES"
    ><button class="button secondary" :disabled="!data" @click="exportar">
      <SirsIcone nome="download" :tamanho="16" />Exportar histórico
    </button></SirsCabecalho
  >
  <form class="filter-bar period-inputs" @submit.prevent="aplicar">
    <label>De<input v-model="de" type="date" required /></label
    ><label>Até<input v-model="ate" type="date" required /></label
    ><button class="button primary" type="submit">Aplicar período</button>
  </form>
  <p v-if="aviso" class="inline-alert" role="alert">{{ aviso }}</p>
  <SirsEstado :carregando="status === 'pending'" :erro="error" @tentar="refresh"
    ><template v-if="data"
      ><div class="metrics-grid">
        <SirsMetrica
          titulo="Vagas fechadas"
          :valor="formatarNumero(data.resumo.vagasFechadas)"
          descricao="Concluídas no período"
          icone="success"
          cor="green"
        /><SirsMetrica
          titulo="Fechadas no prazo"
          :valor="formatarNumero(data.resumo.percentualNoPrazo, 1) + '%'"
          :descricao="'Meta: ' + (data.resumo.metasVigentes.VAGAS_NO_PRAZO ?? '—') + '%'"
          icone="calendar"
        /><SirsMetrica
          titulo="Custo por vaga"
          :valor="formatarMoeda(data.resumo.custoPorVagaFechada)"
          descricao="Média das vagas fechadas"
          icone="wallet"
          cor="blue"
        /><SirsMetrica
          titulo="Tempo até a admissão"
          :valor="formatarNumero(data.resumo.mediaDiasAdmissao, 1) + ' dias'"
          descricao="Média das contratações no período"
          icone="history"
          cor="orange"
        />
      </div>
      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>Evolução das vagas concluídas</h2>
            <p>{{ formatarData(periodo.de) }} a {{ formatarData(periodo.ate) }}</p>
          </div>
        </div>
        <div class="panel-body">
          <div class="chart-bars" role="img" aria-label="Quantidade de vagas concluídas por mês">
            <div v-for="mes in data.historico" :key="mes.de" class="chart-column">
              <strong>{{ mes.vagasFechadas }}</strong>
              <div
                :style="{ height: Math.max(2, (mes.vagasFechadas / maiorVolume) * 120) + 'px' }"
              />
              <small>{{ mes.de.slice(5, 7) }}/{{ mes.de.slice(2, 4) }}</small>
            </div>
          </div>
        </div>
      </section>
      <div class="section-tabs">
        <button
          v-for="item in [
            { chave: 'cargos', titulo: 'Por cargo' },
            { chave: 'recrutadores', titulo: 'Por recrutador' },
            ...(autenticacao.usuario.value?.perfil !== 'GESTOR'
              ? [{ chave: 'canais', titulo: 'Por canal' }]
              : []),
          ]"
          :key="item.chave"
          class="tab-button"
          :class="{ active: aba === item.chave }"
          @click="aba = item.chave"
        >
          {{ item.titulo }}
        </button>
      </div>
      <section class="panel">
        <SirsTabela
          v-if="aba === 'cargos' && data.cargos.length"
          :registros="data.cargos"
          :colunas="[
            { chave: 'nome', titulo: 'Cargo' },
            { chave: 'posicoesAbertas', titulo: 'Posições abertas' },
            { chave: 'candidatos', titulo: 'Candidatos' },
            { chave: 'mediaDiasAberto', titulo: 'Dias em aberto' },
            { chave: 'investimentoMedio', titulo: 'Investimento médio' },
          ]"
          legenda="Indicadores por cargo"
          ><template #celula-mediaDiasAberto="{ registro }">{{
            formatarNumero(registro.mediaDiasAberto, 1)
          }}</template
          ><template #celula-investimentoMedio="{ registro }">{{
            formatarMoeda(registro.investimentoMedio)
          }}</template></SirsTabela
        ><SirsTabela
          v-else-if="aba === 'recrutadores' && data.recrutadores.length"
          :registros="data.recrutadores"
          :colunas="[
            { chave: 'nome', titulo: 'Recrutador' },
            { chave: 'posicoesTrabalhadas', titulo: 'Posições trabalhadas' },
            { chave: 'posicoesPreenchidas', titulo: 'Preenchidas' },
            { chave: 'percentualFechamento', titulo: 'Fechamento (%)' },
          ]"
          legenda="Indicadores por recrutador"
        /><SirsTabela
          v-else-if="aba === 'canais' && data.canais.length"
          :registros="data.canais"
          :colunas="[
            { chave: 'nome', titulo: 'Canal' },
            { chave: 'visitas', titulo: 'Visitas' },
            { chave: 'contratacoes', titulo: 'Contratações' },
            { chave: 'percentualCanal', titulo: 'Conversão (%)' },
          ]"
          legenda="Indicadores por canal"
        /><SirsEstado
          v-else
          vazio
          titulo="Ainda não há indicadores neste recorte"
          descricao="Os indicadores serão calculados conforme os processos forem movimentados."
        /></section></template
  ></SirsEstado>
</template>
