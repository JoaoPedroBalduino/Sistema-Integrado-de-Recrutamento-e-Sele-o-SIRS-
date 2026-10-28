<script setup lang="ts">
import type { CampoFormulario, ValoresFormulario } from '#shared/types/formulario'
const props = defineProps<{
  campos: CampoFormulario[]
  inicial?: Record<string, unknown>
  salvando?: boolean
  erro?: unknown
  rotulo?: string
}>()
defineEmits<{ salvar: [valores: ValoresFormulario]; cancelar: [] }>()
const identificadorFormulario = useId()
const valores = reactive<ValoresFormulario>({})
watch(
  () => [props.campos, props.inicial],
  () => {
    for (const campo of props.campos)
      valores[campo.chave] = (props.inicial?.[campo.chave] ??
        campo.valor ??
        (campo.tipo === 'checkbox'
          ? false
          : campo.tipo === 'multiple'
            ? []
            : '')) as ValoresFormulario[string]
  },
  { immediate: true },
)
const erros = computed(() => (props.erro ? descreverErro(props.erro) : null))
function preparar() {
  return Object.fromEntries(
    Object.entries(valores).map(([chave, valor]) => [chave, valor === '' ? null : valor]),
  ) as ValoresFormulario
}
</script>
<template>
  <form class="resource-form" @submit.prevent="$emit('salvar', preparar())">
    <div v-if="erros" class="inline-alert" role="alert">{{ erros.mensagem }}</div>
    <div class="form-grid">
      <div
        v-for="campo in campos"
        :key="campo.chave"
        class="field"
        :class="{ 'full-width': campo.tipo === 'textarea' || campo.tipo === 'multiple' }"
      >
        <label :for="identificadorFormulario + '-' + campo.chave"
          >{{ campo.rotulo
          }}<span v-if="campo.obrigatorio" class="required-mark" aria-hidden="true" /></label
        ><textarea
          v-if="campo.tipo === 'textarea'"
          :id="identificadorFormulario + '-' + campo.chave"
          :value="String(valores[campo.chave] ?? '')"
          @input="valores[campo.chave] = ($event.target as HTMLTextAreaElement).value"
          rows="3"
          :required="campo.obrigatorio"
          :maxlength="campo.maximo"
        /><select
          v-else-if="campo.tipo === 'select' || campo.tipo === 'multiple'"
          :id="identificadorFormulario + '-' + campo.chave"
          v-model="valores[campo.chave]"
          :multiple="campo.tipo === 'multiple'"
          :required="campo.obrigatorio"
        >
          <option v-if="campo.tipo !== 'multiple'" value="">Selecione</option>
          <option v-for="opcao in campo.opcoes" :key="opcao.valor" :value="opcao.valor">
            {{ opcao.rotulo }}
          </option></select
        ><input
          v-else-if="campo.tipo === 'checkbox'"
          :id="identificadorFormulario + '-' + campo.chave"
          v-model="valores[campo.chave]"
          type="checkbox"
        /><input
          v-else
          :id="identificadorFormulario + '-' + campo.chave"
          v-model="valores[campo.chave]"
          :type="campo.tipo || 'text'"
          :required="campo.obrigatorio"
          :min="campo.minimo"
          :max="campo.tipo === 'number' || campo.tipo === 'date' ? campo.maximo : undefined"
          :minlength="campo.tipo === 'password' ? campo.minimo : undefined"
          :maxlength="campo.tipo !== 'number' ? campo.maximo : undefined"
          :step="campo.passo"
          :autocomplete="campo.tipo === 'password' ? 'new-password' : undefined"
          :aria-invalid="Boolean(erros?.campos[campo.chave])"
        /><small v-if="campo.dica">{{ campo.dica }}</small
        ><small v-if="erros?.campos[campo.chave]" class="field-error">{{
          erros.campos[campo.chave]
        }}</small>
      </div>
    </div>
    <div class="form-footer">
      <button
        type="button"
        class="button secondary"
        :disabled="salvando"
        @click="$emit('cancelar')"
      >
        Cancelar</button
      ><button class="button primary" type="submit" :disabled="salvando">
        <SirsIcone
          :nome="salvando ? 'loading' : 'check'"
          :class="{ spin: salvando }"
          :tamanho="17"
        />{{ salvando ? 'Salvando…' : rotulo || 'Salvar alterações' }}
      </button>
    </div>
  </form>
</template>
