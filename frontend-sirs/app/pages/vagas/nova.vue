<script setup lang="ts">
import type { RespostaVaga } from '#shared/types/api'
import type { ValoresFormulario } from '#shared/types/formulario'
import { perfisEdicaoVagas } from '~/utils/permissoes'
definePageMeta({ perfis: perfisEdicaoVagas })
useHead({ title: 'Nova vaga' })
const consulta = useApiSirs()
const salvando = ref(false)
const erro = shallowRef<unknown>(null)
async function salvar(valores: ValoresFormulario) {
  salvando.value = true
  erro.value = null
  try {
    const vaga = await consulta<RespostaVaga>('vagas', { method: 'POST', body: valores })
    useNotificacoes().adicionar('Vaga criada em rascunho.')
    await navigateTo('/vagas/' + vaga.identificador)
  } catch (falha) {
    erro.value = falha
  } finally {
    salvando.value = false
  }
}
</script>
<template>
  <NuxtLink to="/vagas" class="back-link"
    ><SirsIcone nome="back" :tamanho="15" />Voltar para vagas</NuxtLink
  ><SirsCabecalho
    titulo="Nova vaga"
    descricao="Preencha os dados da vaga. Depois, configure as etapas e os critérios."
    categoria="NOVA VAGA"
  />
  <section class="panel">
    <div class="panel-header">
      <h2>Informações da vaga</h2>
      <SirsBadge valor="RASCUNHO" />
    </div>
    <div class="panel-body">
      <VagaFormulario
        :salvando="salvando"
        :erro="erro"
        @salvar="salvar"
        @cancelar="navigateTo('/vagas')"
      />
    </div>
  </section>
</template>
