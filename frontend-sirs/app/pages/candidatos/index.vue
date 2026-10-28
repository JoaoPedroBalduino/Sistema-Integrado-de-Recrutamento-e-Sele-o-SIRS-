<script setup lang="ts">
import type { RespostaCandidato } from '#shared/types/api'
import { perfisCandidatos, perfisEdicaoCandidatos } from '~/utils/permissoes'
import { camposCandidato, prepararCandidato } from '~/utils/candidatos'
import { iniciais, nomesStatus } from '~/utils/formatacao'
definePageMeta({ perfis: perfisCandidatos })
useHead({ title: 'Candidatos' })
const autenticacao = useAutenticacao()
const consulta = useApiSirs()
const pesquisa = ref('')
const pesquisaAplicada = ref('')
const filtros = computed(() => ({
  pesquisa: pesquisaAplicada.value || undefined,
  sort: 'nome,asc',
}))
const { data, status, error, refresh, pagina } = useColecao<RespostaCandidato>(
  'candidatos',
  filtros,
)
const formulario = useFormulario(
  (valores) => consulta('candidatos', { method: 'POST', body: prepararCandidato(valores) }),
  refresh,
)
const colunas = [
  { chave: 'nome', titulo: 'Nome / contato' },
  { chave: 'baseLegal', titulo: 'Base legal' },
  { chave: 'dataColeta', titulo: 'Coleta' },
  { chave: 'acao', titulo: '' },
]
function limparBusca() {
  pesquisa.value = ''
  pesquisaAplicada.value = ''
}
</script>
<template>
  <SirsCabecalho
    titulo="Candidatos"
    descricao="Consulte e mantenha os dados dos candidatos."
    categoria="CANDIDATOS"
    ><button
      v-if="autenticacao.permite(perfisEdicaoCandidatos)"
      class="button primary"
      @click="formulario.abrir()"
    >
      <SirsIcone nome="plus" :tamanho="16" />Novo candidato
    </button></SirsCabecalho
  >
  <form class="filter-bar" role="search" @submit.prevent="pesquisaAplicada = pesquisa.trim()">
    <div class="search-input">
      <SirsIcone nome="search" :tamanho="16" /><label for="pesquisa-candidatos" class="sr-only"
        >Buscar candidato por nome ou e-mail</label
      ><input
        id="pesquisa-candidatos"
        v-model="pesquisa"
        placeholder="Buscar por nome ou e-mail"
        type="search"
      />
    </div>
    <button class="button secondary" type="submit">Buscar</button
    ><button v-if="pesquisaAplicada" class="button secondary" type="button" @click="limparBusca">
      Limpar busca
    </button>
  </form>
  <section class="panel">
    <div class="panel-header">
      <h2>Talentos cadastrados</h2>
      <span class="badge">{{ data?.totalElements || 0 }} pessoas</span>
    </div>
    <SirsEstado
      :carregando="status === 'pending'"
      :erro="error"
      :vazio="!data?.content.length"
      titulo="Nenhum candidato encontrado"
      descricao="Cadastre um candidato ou tente outro termo de busca."
      @tentar="refresh"
      ><SirsTabela
        :registros="data?.content || []"
        :colunas="colunas"
        legenda="Candidatos cadastrados"
        ><template #celula-nome="{ registro }"
          ><div class="table-person">
            <span class="avatar">{{ iniciais(registro.nome) }}</span>
            <div>
              <NuxtLink :to="'/candidatos/' + registro.identificador" class="table-primary">{{
                registro.nome
              }}</NuxtLink
              ><small>{{ registro.email }}</small>
            </div>
          </div></template
        ><template #celula-baseLegal="{ registro }">{{ nomesStatus[registro.baseLegal] }}</template
        ><template #celula-dataColeta="{ registro }">{{
          formatarData(registro.dataColeta)
        }}</template
        ><template #celula-acao="{ registro }"
          ><NuxtLink :to="'/candidatos/' + registro.identificador" class="text-link"
            >Ver ficha<SirsIcone
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
  <SirsModal
    :aberto="formulario.aberto.value"
    titulo="Cadastrar candidato"
    @fechar="formulario.fechar"
    ><SirsFormulario
      :campos="camposCandidato"
      :salvando="formulario.salvando.value"
      :erro="formulario.erroFormulario.value"
      @salvar="formulario.salvar"
      @cancelar="formulario.fechar"
  /></SirsModal>
</template>
