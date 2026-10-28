<script setup lang="ts">
import type {
  RespostaCandidatura,
  RespostaVaga,
  RespostaCandidato,
  RespostaCadastroApoio,
} from '#shared/types/api'
import type { CampoFormulario } from '#shared/types/formulario'
import { iniciais, formatarNumero } from '~/utils/formatacao'
definePageMeta({ perfis: ['ADMIN', 'COORD_RH', 'RECRUTADOR', 'GESTOR', 'AVALIADOR'] })
useHead({ title: 'Candidaturas' })
const autenticacao = useAutenticacao()
const consulta = useApiSirs()
const listar = useReferencias()
const podeCadastrar = computed(() => autenticacao.permite(['COORD_RH', 'RECRUTADOR', 'GESTOR']))
const vagaId = ref('')
const filtroStatus = ref('')
const visualizacao = ref<'kanban' | 'lista'>('kanban')
const filtros = computed(() => ({
  vagaId: vagaId.value || undefined,
  status: filtroStatus.value || undefined,
}))
const { data, status, error, refresh, pagina } = useColecao<RespostaCandidatura>(
  'candidaturas',
  filtros,
)
const { data: vagas } = await useAsyncData('vagas-candidaturas', () =>
  autenticacao.usuario.value?.perfil === 'AVALIADOR'
    ? Promise.resolve([] as RespostaVaga[])
    : listar<RespostaVaga>('vagas'),
)
const referencias = ref<{ candidatos: RespostaCandidato[]; canais: RespostaCadastroApoio[] }>({
  candidatos: [],
  canais: [],
})
const erroReferencias = ref('')
const formulario = useFormulario(
  (valores) => consulta('candidaturas', { method: 'POST', body: valores }),
  refresh,
)
async function cadastrar() {
  erroReferencias.value = ''
  try {
    const [candidatos, canais] = await Promise.all([
      listar<RespostaCandidato>('candidatos'),
      listar<RespostaCadastroApoio>('cadastros/canais'),
    ])
    referencias.value = { candidatos, canais }
    formulario.abrir()
  } catch (falha) {
    erroReferencias.value = descreverErro(falha).mensagem
  }
}
const campos = computed<CampoFormulario[]>(() => [
  {
    chave: 'vagaId',
    rotulo: 'Vaga publicada',
    tipo: 'select',
    obrigatorio: true,
    opcoes:
      vagas.value
        ?.filter((vaga) => vaga.status === 'PUBLICADA')
        .map((vaga) => ({ valor: vaga.identificador, rotulo: vaga.titulo })) || [],
  },
  {
    chave: 'candidatoId',
    rotulo: 'Candidato',
    tipo: 'select',
    obrigatorio: true,
    opcoes: referencias.value.candidatos.map((candidato) => ({
      valor: candidato.identificador,
      rotulo: candidato.nome + ' · ' + candidato.email,
    })),
  },
  {
    chave: 'canalId',
    rotulo: 'Canal de origem',
    tipo: 'select',
    obrigatorio: true,
    opcoes: referencias.value.canais
      .filter((canal) => canal.ativo)
      .map((canal) => ({ valor: canal.identificador, rotulo: canal.nome })),
  },
])
const grupos = computed(() =>
  [
    ...new Set(
      data.value?.content.map((candidatura) => candidatura.etapaAtual || 'Sem etapa') || [],
    ),
  ].map((nome) => ({
    nome,
    candidaturas: data.value!.content.filter(
      (candidatura) => (candidatura.etapaAtual || 'Sem etapa') === nome,
    ),
  })),
)
const colunas = [
  { chave: 'nomeCandidato', titulo: 'Candidato' },
  { chave: 'tituloVaga', titulo: 'Vaga' },
  { chave: 'etapaAtual', titulo: 'Etapa' },
  { chave: 'status', titulo: 'Situação' },
  { chave: 'notaFinal', titulo: 'Nota final' },
]
</script>
<template>
  <SirsCabecalho
    titulo="Candidaturas"
    descricao="Acompanhe as etapas e as avaliações de cada candidato."
    categoria="CANDIDATURAS"
    ><button v-if="podeCadastrar" class="button primary" @click="cadastrar">
      <SirsIcone nome="plus" :tamanho="16" />Nova candidatura
    </button></SirsCabecalho
  >
  <p v-if="erroReferencias" class="inline-alert" role="alert">{{ erroReferencias }}</p>
  <div class="filter-bar">
    <label class="sr-only" for="filtro-vaga">Filtrar por vaga</label
    ><select v-if="vagas?.length" id="filtro-vaga" v-model="vagaId">
      <option value="">Todas as vagas</option>
      <option v-for="vaga in vagas" :key="vaga.identificador" :value="vaga.identificador">
        {{ vaga.titulo }}
      </option></select
    ><label class="sr-only" for="filtro-status">Filtrar por situação</label
    ><select id="filtro-status" v-model="filtroStatus">
      <option value="">Todas as situações</option>
      <option value="EM_AVALIACAO">Em avaliação</option>
      <option value="APROVADO">Aprovados</option>
      <option value="REPROVADO">Reprovados</option>
    </select>
    <div class="filter-tabs" style="margin: 0; margin-left: auto">
      <button :class="{ active: visualizacao === 'kanban' }" @click="visualizacao = 'kanban'">
        <SirsIcone nome="kanban" :tamanho="14" />Painel</button
      ><button :class="{ active: visualizacao === 'lista' }" @click="visualizacao = 'lista'">
        Lista
      </button>
    </div>
  </div>
  <SirsEstado
    :carregando="status === 'pending'"
    :erro="error"
    :vazio="!data?.content.length"
    titulo="Novas conexões começam aqui"
    descricao="As candidaturas aparecerão após a inscrição em uma vaga publicada."
    @tentar="refresh"
    ><div v-if="visualizacao === 'kanban'" class="kanban-board">
      <section v-for="grupo in grupos" :key="grupo.nome" class="kanban-column">
        <h2 class="kanban-column-header">
          {{ grupo.nome }}<span>{{ grupo.candidaturas.length }}</span>
        </h2>
        <NuxtLink
          v-for="candidatura in grupo.candidaturas"
          :key="candidatura.identificador"
          :to="'/candidaturas/' + candidatura.identificador"
          class="candidate-card"
          ><span class="avatar">{{ iniciais(candidatura.nomeCandidato) }}</span>
          <h3>{{ candidatura.nomeCandidato }}</h3>
          <p>{{ candidatura.tituloVaga }}</p>
          <div class="candidate-card-footer">
            <SirsBadge :valor="candidatura.status" /><span class="score">{{
              formatarNota(candidatura.notaFinal)
            }}</span>
          </div></NuxtLink
        >
      </section>
    </div>
    <section v-else class="panel">
      <SirsTabela :registros="data?.content || []" :colunas="colunas" legenda="Candidaturas"
        ><template #celula-nomeCandidato="{ registro }"
          ><NuxtLink :to="'/candidaturas/' + registro.identificador" class="table-primary">{{
            registro.nomeCandidato
          }}</NuxtLink
          ><small>{{ registro.emailCandidato }}</small></template
        ><template #celula-status="{ registro }"><SirsBadge :valor="registro.status" /></template
        ><template #celula-notaFinal="{ registro }">{{
          formatarNota(registro.notaFinal)
        }}</template></SirsTabela
      >
    </section></SirsEstado
  ><SirsPaginacao
    v-if="data"
    :pagina="pagina"
    :total="data.totalElements"
    :total-paginas="data.totalPages"
    @mudar="pagina = $event"
  />
  <p class="page-description">
    O painel agrupa as candidaturas da página atual por etapa. Use os filtros e a paginação para
    percorrer todos os registros.
  </p>
  <SirsModal
    :aberto="formulario.aberto.value"
    titulo="Inscrever um talento"
    @fechar="formulario.fechar"
    ><SirsFormulario
      :campos="campos"
      :salvando="formulario.salvando.value"
      :erro="formulario.erroFormulario.value"
      rotulo="Criar candidatura"
      @salvar="formulario.salvar"
      @cancelar="formulario.fechar"
  /></SirsModal>
</template>
