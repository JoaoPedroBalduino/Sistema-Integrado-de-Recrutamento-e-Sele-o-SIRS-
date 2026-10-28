<script setup lang="ts">
import type { RespostaCandidato } from '#shared/types/api'
import { perfisCandidatos, perfisEdicaoCandidatos } from '~/utils/permissoes'
import { camposCandidato, prepararCandidato } from '~/utils/candidatos'
import { nomesStatus, formatarData, iniciais } from '~/utils/formatacao'
definePageMeta({ perfis: perfisCandidatos })
useHead({ title: 'Ficha do candidato' })
const rota = useRoute()
const consulta = useApiSirs()
const autenticacao = useAutenticacao()
const caminho = computed(() => 'candidatos/' + rota.params.identificador)
const { data, status, error, refresh } = await useAsyncData(
  () => caminho.value,
  () => consulta<RespostaCandidato>(caminho.value),
)
const formulario = useFormulario(
  (valores) => consulta(caminho.value, { method: 'PUT', body: prepararCandidato(valores) }),
  refresh,
)
function editar() {
  if (!data.value) return
  const instante = new Date(data.value.dataColeta)
  const local = new Date(instante.getTime() - instante.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16)
  formulario.abrir({ ...data.value, dataColeta: local })
}
const curriculoSeguro = computed(() =>
  data.value?.curriculoUrl?.startsWith('https://') ? data.value.curriculoUrl : null,
)
</script>
<template>
  <NuxtLink to="/candidatos" class="back-link"
    ><SirsIcone nome="back" :tamanho="15" />Banco de talentos</NuxtLink
  ><SirsEstado :carregando="status === 'pending'" :erro="error" @tentar="refresh"
    ><template v-if="data"
      ><SirsCabecalho :titulo="data.nome" :descricao="data.email" categoria="FICHA DO CANDIDATO"
        ><a
          v-if="curriculoSeguro"
          :href="curriculoSeguro"
          target="_blank"
          rel="noopener noreferrer"
          class="button secondary"
          ><SirsIcone nome="file" :tamanho="15" />Abrir currículo</a
        ><button
          v-if="autenticacao.permite(perfisEdicaoCandidatos) && !data.anonimizadoEm"
          class="button primary"
          @click="editar"
        >
          Editar cadastro
        </button></SirsCabecalho
      >
      <section class="panel">
        <div class="panel-header">
          <div class="table-person">
            <span class="avatar">{{ iniciais(data.nome) }}</span>
            <h2>Informações e consentimento</h2>
          </div>
          <span class="badge">{{ data.anonimizadoEm ? 'Desidentificado' : 'Cadastro ativo' }}</span>
        </div>
        <div class="panel-body">
          <dl class="information-grid">
            <div>
              <dt>Nome</dt>
              <dd>{{ data.nome }}</dd>
            </div>
            <div>
              <dt>E-mail</dt>
              <dd>{{ data.email }}</dd>
            </div>
            <div>
              <dt>Base legal</dt>
              <dd>{{ nomesStatus[data.baseLegal] }}</dd>
            </div>
            <div>
              <dt>Data da coleta</dt>
              <dd>{{ formatarData(data.dataColeta) }}</dd>
            </div>
            <div>
              <dt>Consentimento</dt>
              <dd>{{ data.versaoConsentimento || 'Não aplicável' }}</dd>
            </div>
            <div>
              <dt>Desidentificação</dt>
              <dd>{{ formatarData(data.anonimizadoEm) }}</dd>
            </div>
          </dl>
        </div>
      </section></template
    ></SirsEstado
  ><SirsModal
    :aberto="formulario.aberto.value"
    titulo="Editar candidato"
    @fechar="formulario.fechar"
    ><SirsFormulario
      :campos="camposCandidato"
      :inicial="formulario.inicial.value"
      :salvando="formulario.salvando.value"
      :erro="formulario.erroFormulario.value"
      @salvar="formulario.salvar"
      @cancelar="formulario.fechar"
  /></SirsModal>
</template>
