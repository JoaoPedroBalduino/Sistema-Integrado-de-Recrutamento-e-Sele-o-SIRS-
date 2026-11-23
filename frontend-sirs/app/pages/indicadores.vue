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
const abas = [
  { chave: 'cargos', titulo: 'Por cargo' },
  { chave: 'recrutadores', titulo: 'Por recrutador' },
  { chave: 'canais', titulo: 'Por canal' },
] as const
const aba = ref<(typeof abas)[number]['chave']>('cargos')
const abasVisiveis = computed(() =>
  abas.filter((item) => item.chave !== 'canais' || autenticacao.usuario.value?.perfil !== 'GESTOR'),
)
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
const {
  data: resumo,
  status: statusResumo,
  error: erroResumo,
  refresh: atualizarResumo,
} = useLazyAsyncData(
  'indicadores-resumo',
  () => consulta<RespostaResumoIndicadores>('indicadores/resumo', { query: periodo.value }),
  { watch: [periodo] },
)
const {
  data: historico,
  status: statusHistorico,
  error: erroHistorico,
  refresh: atualizarHistorico,
} = useLazyAsyncData(
  'indicadores-historico',
  () => consulta<RespostaResumoIndicadores[]>('indicadores/historico', { query: periodo.value }),
  { server: false, watch: [periodo] },
)
const {
  data: recorte,
  status: statusRecorte,
  error: erroRecorte,
  refresh: atualizarRecorte,
} = useLazyAsyncData(
  'indicadores-recorte',
  async () => {
    const tipo = aba.value
    const opcoes = { query: periodo.value }
    switch (tipo) {
      case 'cargos':
        return {
          tipo,
          registros: await consulta<RespostaIndicadorCargo[]>('indicadores/cargos', opcoes),
        }
      case 'recrutadores':
        return {
          tipo,
          registros: await consulta<RespostaIndicadorRecrutador[]>(
            'indicadores/recrutadores',
            opcoes,
          ),
        }
      case 'canais':
        return {
          tipo,
          registros: await consulta<RespostaIndicadorCanal[]>('indicadores/canais', opcoes),
        }
    }
  },
  { server: false, watch: [aba, periodo] },
)

function exportar() {
  if (statusHistorico.value !== 'success' || !historico.value) return
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
        historico.value.map((mes) => [
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
    ><button class="button secondary" :disabled="statusHistorico !== 'success'" @click="exportar">
      <SirsIcone nome="download" :tamanho="16" />Exportar histórico
    </button></SirsCabecalho
  >
  <form class="filter-bar period-inputs" @submit.prevent="aplicar">
    <label>De<input v-model="de" type="date" required /></label
    ><label>Até<input v-model="ate" type="date" required /></label
    ><button class="button primary" type="submit">Aplicar período</button>
  </form>
  <p v-if="aviso" class="inline-alert" role="alert">{{ aviso }}</p>
  <SirsEstado
    :carregando="statusResumo === 'idle' || statusResumo === 'pending'"
    :erro="erroResumo"
    @tentar="atualizarResumo"
    ><template v-if="resumo"
      ><div class="metrics-grid">
        <SirsMetrica
          titulo="Vagas fechadas"
          :valor="formatarNumero(resumo.vagasFechadas)"
          descricao="Concluídas no período"
        /><SirsMetrica
          titulo="Fechadas no prazo"
          :valor="formatarNumero(resumo.percentualNoPrazo, 1) + '%'"
          :descricao="'Meta: ' + (resumo.metasVigentes.VAGAS_NO_PRAZO ?? '—') + '%'"
        /><SirsMetrica
          titulo="Custo por vaga"
          :valor="formatarMoeda(resumo.custoPorVagaFechada)"
          descricao="Média das vagas fechadas"
        /><SirsMetrica
          titulo="Tempo até a admissão"
          :valor="formatarNumero(resumo.mediaDiasAdmissao, 1) + ' dias'"
          descricao="Média das contratações no período"
        />
      </div>
    </template>
  </SirsEstado>
  <section class="panel">
    <div class="panel-header">
      <div>
        <h2>Evolução das vagas concluídas</h2>
        <p>{{ formatarData(periodo.de) }} a {{ formatarData(periodo.ate) }}</p>
      </div>
    </div>
    <div class="panel-body">
      <SirsEstado
        :carregando="statusHistorico === 'idle' || statusHistorico === 'pending'"
        :erro="erroHistorico"
        @tentar="atualizarHistorico"
      >
        <SirsGraficoHistorico
          v-if="historico"
          :periodos="historico"
          titulo="Quantidade de vagas concluídas por mês"
        />
      </SirsEstado>
    </div>
  </section>
  <div class="section-tabs">
    <button
      v-for="item in abasVisiveis"
      :key="item.chave"
      class="tab-button"
      :class="{ active: aba === item.chave }"
      @click="aba = item.chave"
    >
      {{ item.titulo }}
    </button>
  </div>
  <section class="panel">
    <SirsEstado
      :carregando="statusRecorte === 'idle' || statusRecorte === 'pending'"
      :erro="erroRecorte"
      @tentar="atualizarRecorte"
    >
      <SirsTabela
        v-if="recorte?.tipo === 'cargos' && recorte.registros.length"
        :registros="recorte.registros"
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
        v-else-if="recorte?.tipo === 'recrutadores' && recorte.registros.length"
        :registros="recorte.registros"
        :colunas="[
          { chave: 'nome', titulo: 'Recrutador' },
          { chave: 'posicoesTrabalhadas', titulo: 'Posições trabalhadas' },
          { chave: 'posicoesPreenchidas', titulo: 'Preenchidas' },
          { chave: 'percentualFechamento', titulo: 'Fechamento (%)' },
        ]"
        legenda="Indicadores por recrutador"
      /><SirsTabela
        v-else-if="recorte?.tipo === 'canais' && recorte.registros.length"
        :registros="recorte.registros"
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
      />
    </SirsEstado>
  </section>
</template>
