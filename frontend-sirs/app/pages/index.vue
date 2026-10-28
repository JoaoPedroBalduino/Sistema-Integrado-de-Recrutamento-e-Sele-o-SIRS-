<script setup lang="ts">
import type {
  RespostaPaginada,
  RespostaVaga,
  RespostaCandidatura,
  RespostaResumoIndicadores,
} from '#shared/types/api'
import { formatarNumero, formatarMoeda, formatarData, dataHoje, iniciais } from '~/utils/formatacao'
import { perfisEdicaoVagas } from '~/utils/permissoes'
useHead({ title: 'Visão geral' })
const autenticacao = useAutenticacao()
const consulta = useApiSirs()
const hoje = dataHoje()
const inicio = hoje.slice(0, 4) + '-01-01'
const {
  data: dados,
  status,
  error,
  refresh,
} = await useAsyncData('painel-inicial', async () => {
  if (autenticacao.usuario.value?.perfil === 'AVALIADOR') return null
  const [vagas, publicadas, aprovacao, resumo, historico] = await Promise.all([
    consulta<RespostaPaginada<RespostaVaga>>('vagas', { query: { size: 5, sort: 'id,desc' } }),
    consulta<RespostaPaginada<RespostaVaga>>('vagas', { query: { size: 1, status: 'PUBLICADA' } }),
    consulta<RespostaPaginada<RespostaVaga>>('vagas', {
      query: { size: 1, status: 'EM_APROVACAO' },
    }),
    consulta<RespostaResumoIndicadores>('indicadores/resumo', { query: { de: inicio, ate: hoje } }),
    consulta<RespostaResumoIndicadores[]>('indicadores/historico', {
      query: { de: inicio, ate: hoje },
    }),
  ])
  const candidaturas =
    autenticacao.usuario.value?.perfil === 'DIRETORIA'
      ? null
      : await consulta<RespostaPaginada<RespostaCandidatura>>('candidaturas', {
          query: { size: 4, sort: 'id,desc' },
        })
  return { vagas, publicadas, aprovacao, resumo, historico, candidaturas }
})
const maiorVolume = computed(() =>
  Math.max(1, ...(dados.value?.historico.map((mes) => mes.vagasFechadas) || [])),
)
const primeiroNome = computed(() => autenticacao.usuario.value?.nome.split(' ')[0] || '')
const colunas = [
  { chave: 'titulo', titulo: 'Vaga / departamento' },
  { chave: 'status', titulo: 'Status' },
  { chave: 'candidatos', titulo: 'Candidatos' },
  { chave: 'prazo', titulo: 'Abertura' },
  { chave: 'acao', titulo: '' },
]
</script>
<template>
  <SirsCabecalho
    :titulo="`Olá, ${primeiroNome}`"
    descricao="Veja o andamento das vagas e as últimas candidaturas."
    ><span class="date-pill"
      ><SirsIcone nome="calendar" :tamanho="15" />{{ formatarData(hoje) }}</span
    ><NuxtLink
      v-if="autenticacao.permite(perfisEdicaoVagas)"
      to="/vagas/nova"
      class="button primary"
      ><SirsIcone nome="plus" :tamanho="16" />Nova vaga</NuxtLink
    ></SirsCabecalho
  >
  <div v-if="autenticacao.usuario.value?.perfil === 'AVALIADOR'" class="panel">
    <SirsEstado
      vazio
      titulo="Avaliações designadas"
      descricao="Acesse as candidaturas designadas para consultar as fichas e registrar suas avaliações."
      ><template #acao
        ><NuxtLink to="/candidaturas" class="button primary"
          >Ver minhas avaliações<SirsIcone nome="next" :tamanho="16" /></NuxtLink></template
    ></SirsEstado>
  </div>
  <SirsEstado v-else :carregando="status === 'pending'" :erro="error" @tentar="refresh"
    ><template v-if="dados"
      ><div class="metrics-grid">
        <SirsMetrica
          titulo="Vagas publicadas"
          :valor="formatarNumero(dados.publicadas.totalElements)"
          descricao="Recebendo candidaturas"
        /><SirsMetrica
          titulo="Vagas em aprovação"
          :valor="formatarNumero(dados.aprovacao.totalElements)"
          descricao="Aguardando aprovação"
        /><SirsMetrica
          titulo="Vagas fechadas"
          :valor="formatarNumero(dados.resumo.vagasFechadas)"
          descricao="No ano atual"
        /><SirsMetrica
          titulo="Custo por vaga"
          :valor="formatarMoeda(dados.resumo.custoPorVagaFechada)"
          descricao="Média das vagas fechadas"
        />
      </div>
      <div v-if="dados.aprovacao.totalElements" class="summary-strip">
        <SirsIcone nome="alert" :tamanho="18" /><span
          ><strong
            >{{ dados.aprovacao.totalElements }}
            {{ dados.aprovacao.totalElements === 1 ? 'vaga aguarda' : 'vagas aguardam' }}
            aprovação.</strong
          >
          Confira as solicitações pendentes.</span
        ><NuxtLink to="/vagas?status=EM_APROVACAO" class="text-link"
          >Revisar vagas<SirsIcone nome="next" :tamanho="14"
        /></NuxtLink>
      </div>
      <div class="dashboard-workspace">
        <div class="dashboard-main">
          <section class="panel">
            <div class="panel-header">
              <div>
                <h2>Vagas recentes</h2>
                <p>Últimas vagas cadastradas.</p>
              </div>
              <NuxtLink to="/vagas">Todas as vagas<SirsIcone nome="next" :tamanho="14" /></NuxtLink>
            </div>
            <SirsEstado
              :vazio="!dados.vagas.content.length"
              titulo="Nenhuma vaga cadastrada"
              descricao="Cadastre uma vaga para iniciar o processo seletivo."
            /><SirsTabela
              v-if="dados.vagas.content.length"
              :registros="dados.vagas.content"
              :colunas="colunas"
              legenda="Vagas recentes"
              ><template #celula-titulo="{ registro }"
                ><NuxtLink :to="'/vagas/' + registro.identificador" class="table-primary">{{
                  registro.titulo
                }}</NuxtLink
                ><small
                  >{{ registro.area.nome }} ·
                  {{ registro.remoto ? 'Remoto' : registro.cidade || 'Local não informado' }}</small
                ></template
              ><template #celula-status="{ registro }"
                ><SirsBadge :valor="registro.status" /></template
              ><template #celula-candidatos="{ registro }"
                ><strong>{{ registro.candidatos }}</strong> candidatos</template
              ><template #celula-prazo="{ registro }">{{
                formatarData(registro.dataAbertura)
              }}</template
              ><template #celula-acao="{ registro }"
                ><NuxtLink
                  :to="'/vagas/' + registro.identificador"
                  class="icon-button"
                  :aria-label="'Abrir vaga ' + registro.titulo"
                  ><SirsIcone nome="chevron" :tamanho="16" /></NuxtLink></template
            ></SirsTabela>
          </section>
          <section class="panel history-panel">
            <div class="panel-header">
              <div>
                <h2>Vagas fechadas por mês</h2>
              </div>
              <span class="period-label">Ano {{ hoje.slice(0, 4) }}</span>
            </div>
            <div class="panel-body">
              <div class="chart-bars" role="img" aria-label="Vagas fechadas em cada mês do ano">
                <div v-for="mes in dados.historico" :key="mes.de" class="chart-column">
                  <strong>{{ mes.vagasFechadas }}</strong>
                  <div
                    :style="{ height: `${Math.max(2, (mes.vagasFechadas / maiorVolume) * 72)}px` }"
                  />
                  <small>{{
                    new Intl.DateTimeFormat('pt-BR', { month: 'short' })
                      .format(new Date(mes.de + 'T12:00:00Z'))
                      .replace('.', '')
                  }}</small>
                </div>
              </div>
            </div>
          </section>
        </div>
        <aside class="panel recent-applications">
          <div class="panel-header">
            <div>
              <h2>{{ dados.candidaturas ? 'Últimas candidaturas' : 'Eficiência do processo' }}</h2>
              <p>
                {{
                  dados.candidaturas
                    ? 'Recebidas mais recentemente.'
                    : 'Indicadores consolidados do período.'
                }}
              </p>
            </div>
          </div>
          <div class="panel-body">
            <template v-if="dados.candidaturas"
              ><NuxtLink
                v-for="candidatura in dados.candidaturas.content"
                :key="candidatura.identificador"
                :to="'/candidaturas/' + candidatura.identificador"
                class="list-item"
                ><span>{{ iniciais(candidatura.nomeCandidato) }}</span>
                <div>
                  <strong>{{ candidatura.nomeCandidato }}</strong>
                  <p>{{ candidatura.tituloVaga }}</p>
                </div>
                <SirsIcone nome="chevron" :tamanho="15" /></NuxtLink
              ><SirsEstado
                v-if="!dados.candidaturas.content.length"
                vazio
                titulo="Nenhuma candidatura"
                descricao="As candidaturas recebidas serão listadas aqui." /></template
            ><template v-else
              ><div class="list-item">
                <SirsIcone nome="success" />
                <div>
                  <strong>{{ formatarNumero(dados.resumo.percentualNoPrazo, 1) }}% no prazo</strong>
                  <p>Das vagas consideradas no período</p>
                </div>
              </div>
              <div class="list-item">
                <SirsIcone nome="calendar" />
                <div>
                  <strong>{{ formatarNumero(dados.resumo.mediaDiasAdmissao, 1) }} dias</strong>
                  <p>Tempo médio até a admissão</p>
                </div>
              </div></template
            >
          </div>
          <NuxtLink v-if="dados.candidaturas" to="/candidaturas" class="panel-footer-link"
            >Todas as candidaturas<SirsIcone nome="next" :tamanho="15"
          /></NuxtLink>
        </aside></div></template
  ></SirsEstado>
</template>
