<script setup lang="ts">
import type { RespostaVaga, StatusVaga } from '#shared/types/api'
import { perfisOperacionais, perfisEdicaoVagas } from '~/utils/permissoes'
import { nomesStatus, formatarData } from '~/utils/formatacao'
definePageMeta({ perfis: perfisOperacionais })
useHead({ title: 'Vagas' })
const autenticacao = useAutenticacao()
const rota = useRoute()
const statusSelecionado = ref(typeof rota.query.status === 'string' ? rota.query.status : '')
const filtros = computed(() => ({ status: statusSelecionado.value || undefined, sort: 'id,desc' }))
const { data, status, error, refresh, pagina } = useColecao<RespostaVaga>('vagas', filtros)
const estados: Array<StatusVaga | ''> = [
  '',
  'RASCUNHO',
  'EM_APROVACAO',
  'PUBLICADA',
  'CONGELADA',
  'FECHADA',
]
const colunas = [
  { chave: 'titulo', titulo: 'Vaga' },
  { chave: 'area', titulo: 'Departamento' },
  { chave: 'status', titulo: 'Status' },
  { chave: 'candidatos', titulo: 'Candidatos' },
  { chave: 'dataAbertura', titulo: 'Abertura' },
  { chave: 'acao', titulo: '' },
]
</script>
<template>
  <SirsCabecalho
    titulo="Vagas"
    descricao="Cada oportunidade é o começo de uma nova conexão."
    categoria="RECRUTAMENTO"
    ><NuxtLink
      v-if="autenticacao.permite(perfisEdicaoVagas)"
      to="/vagas/nova"
      class="button primary"
      ><SirsIcone nome="plus" :tamanho="16" />Nova vaga</NuxtLink
    ></SirsCabecalho
  >
  <div class="filter-tabs" aria-label="Filtrar por status">
    <button
      v-for="estado in estados"
      :key="estado"
      :class="{ active: statusSelecionado === estado }"
      :aria-pressed="statusSelecionado === estado"
      @click="statusSelecionado = estado"
    >
      {{ estado ? nomesStatus[estado] : 'Todas as vagas' }}
    </button>
  </div>
  <section class="panel">
    <div class="panel-header">
      <h2>
        Oportunidades
        {{ statusSelecionado ? nomesStatus[statusSelecionado]?.toLowerCase() : 'da equipe' }}
      </h2>
      <span class="badge">{{ data?.totalElements || 0 }} vagas</span>
    </div>
    <SirsEstado
      :carregando="status === 'pending'"
      :erro="error"
      :vazio="!data?.content.length"
      titulo="Nenhuma vaga por aqui"
      descricao="Crie uma nova oportunidade ou escolha outro filtro."
      @tentar="refresh"
      ><SirsTabela :registros="data?.content || []" :colunas="colunas" legenda="Vagas da equipe"
        ><template #celula-titulo="{ registro }"
          ><NuxtLink :to="'/vagas/' + registro.identificador" class="table-primary">{{
            registro.titulo
          }}</NuxtLink
          ><small
            >{{
              registro.remoto
                ? 'Remoto'
                : [registro.cidade, registro.estado].filter(Boolean).join(' · ') ||
                  'Local não informado'
            }}
            · {{ registro.posicoes }} {{ registro.posicoes === 1 ? 'posição' : 'posições' }}</small
          ></template
        ><template #celula-area="{ registro }">{{ registro.area.nome }}</template
        ><template #celula-status="{ registro }"><SirsBadge :valor="registro.status" /></template
        ><template #celula-dataAbertura="{ registro }">{{
          formatarData(registro.dataAbertura)
        }}</template
        ><template #celula-acao="{ registro }"
          ><NuxtLink :to="'/vagas/' + registro.identificador" class="text-link"
            >Ver detalhes<SirsIcone
              nome="chevron"
              :tamanho="14" /></NuxtLink></template></SirsTabela></SirsEstado
    ><SirsPaginacao
      v-if="data"
      :pagina="pagina"
      :total="data.totalElements"
      :total-paginas="data.totalPages"
      @mudar="pagina = $event"
    />
  </section>
</template>
