<script setup lang="ts">
import type { RespostaVaga, RespostaEstimativaCusto } from '#shared/types/api'
import { chavesParametroCusto } from '#shared/constants/dominios'
import { formatarMoeda, formatarNumero } from '~/utils/formatacao'
definePageMeta({ perfis: ['ADMIN', 'COORD_RH', 'RECRUTADOR', 'DIRETORIA'] })
useHead({ title: 'Custos' })
const autenticacao = useAutenticacao()
const consulta = useApiSirs()
const listar = useReferencias()
const rota = useRoute()
const { data: vagas, error: erroVagas } = await useAsyncData('vagas-custos', () =>
  listar<RespostaVaga>('vagas'),
)
const vagaId = ref(String(rota.query.vagaId || vagas.value?.[0]?.identificador || ''))
const referencia = ref(dataHoje())
const investimentoRealizado = ref(false)
const { data, status, error, refresh } = await useAsyncData(
  'estimativa-custo',
  () =>
    vagaId.value
      ? consulta<RespostaEstimativaCusto>('vagas/' + vagaId.value + '/custos', {
          query: {
            referencia: referencia.value,
            investimentoRealizado: investimentoRealizado.value,
          },
        })
      : Promise.resolve(null),
  { watch: [vagaId, referencia, investimentoRealizado] },
)
const formulario = useFormulario(
  (valores) =>
    consulta('vagas/' + vagaId.value + '/custos/parametros', {
      method: 'PUT',
      body: { parametros: { [String(valores.chave)]: Number(valores.valor) } },
    }),
  refresh,
)
</script>
<template>
  <SirsCabecalho
    titulo="Custos de recrutamento"
    descricao="Consulte os custos e os parâmetros de cada vaga."
    categoria="CUSTOS DO RECRUTAMENTO"
    ><button
      v-if="
        autenticacao.permite(['COORD_RH']) &&
        vagaId &&
        vagas?.find((vaga) => String(vaga.identificador) === vagaId)?.status !== 'FECHADA'
      "
      class="button secondary"
      @click="formulario.abrir()"
    >
      <SirsIcone nome="settings" :tamanho="16" />Ajustar parâmetro
    </button></SirsCabecalho
  >
  <div class="filter-bar">
    <label class="filter-label" for="custo-vaga">Oportunidade</label
    ><select id="custo-vaga" v-model="vagaId">
      <option value="">Selecione uma vaga</option>
      <option v-for="vaga in vagas" :key="vaga.identificador" :value="String(vaga.identificador)">
        {{ vaga.titulo }}
      </option></select
    ><label class="filter-label" for="custo-referencia">Referência</label
    ><input id="custo-referencia" v-model="referencia" type="date" /><label
      class="filter-label"
      style="display: flex; gap: 8px; align-items: center"
      ><input v-model="investimentoRealizado" type="checkbox" />Usar divulgação realizada</label
    >
  </div>
  <SirsEstado
    :carregando="status === 'pending'"
    :erro="error || erroVagas"
    :vazio="!vagaId"
    titulo="Qual oportunidade vamos analisar?"
    descricao="Selecione uma vaga para consultar a estimativa."
    @tentar="refresh"
    ><template v-if="data"
      ><div class="metrics-grid">
        <SirsMetrica
          titulo="Investimento total"
          :valor="formatarMoeda(data.total)"
          descricao="Estimativa consolidada da vaga"
        /><SirsMetrica
          titulo="Horas e pessoas"
          :valor="formatarMoeda(data.custoHoras)"
          descricao="Custo da dedicação às etapas"
        /><SirsMetrica
          titulo="Investimentos diretos"
          :valor="formatarMoeda(data.investimentosDiretos)"
          descricao="Kit e divulgação conforme parâmetros"
        /><SirsMetrica
          titulo="Dedicação estimada"
          :valor="
            formatarNumero(
              data.etapas.reduce((total, etapa) => total + etapa.horas, 0),
              1,
            ) + ' h'
          "
          descricao="Soma das horas de todas as etapas"
        />
      </div>
      <section class="panel">
        <div class="panel-header">
          <h2>De onde vem o investimento</h2>
          <span class="badge">Estimativa</span>
        </div>
        <SirsTabela
          :registros="data.etapas"
          :colunas="[
            { chave: 'descricao', titulo: 'Etapa / atividade' },
            { chave: 'horas', titulo: 'Horas estimadas' },
            { chave: 'custo', titulo: 'Custo' },
          ]"
          legenda="Composição dos custos"
          ><template #celula-horas="{ registro }"
            >{{ formatarNumero(registro.horas, 2) }} h</template
          ><template #celula-custo="{ registro }">{{
            formatarMoeda(registro.custo)
          }}</template></SirsTabela
        >
      </section>
      <p class="inline-note">
        A estimativa é calculada pelo backend com os parâmetros vigentes na data de referência.
        Horas e investimentos não são recalculados no navegador.
      </p></template
    ></SirsEstado
  ><SirsModal
    :aberto="formulario.aberto.value"
    titulo="Ajustar parâmetro desta vaga"
    @fechar="formulario.fechar"
    ><SirsFormulario
      :campos="[
        {
          chave: 'chave',
          rotulo: 'Parâmetro',
          tipo: 'select',
          obrigatorio: true,
          opcoes: chavesParametroCusto.map((chave) => ({
            valor: chave,
            rotulo: chave.toLowerCase().replaceAll('_', ' '),
          })),
        },
        {
          chave: 'valor',
          rotulo: 'Novo valor',
          tipo: 'number',
          obrigatorio: true,
          minimo: 0,
          passo: '0.0001',
        },
      ]"
      :salvando="formulario.salvando.value"
      :erro="formulario.erroFormulario.value"
      @salvar="formulario.salvar"
      @cancelar="formulario.fechar"
  /></SirsModal>
</template>
