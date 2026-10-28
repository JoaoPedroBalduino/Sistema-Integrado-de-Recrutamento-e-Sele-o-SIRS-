<script setup lang="ts" generic="Registro extends object">
defineProps<{
  colunas: Array<{ chave: string; titulo: string }>
  registros: Registro[]
  legenda: string
}>()
function valor(registro: Registro, chave: string) {
  return (registro as Record<string, unknown>)[chave] ?? '—'
}
</script>
<template>
  <div class="table-scroll">
    <table>
      <caption class="sr-only">
        {{
          legenda
        }}
      </caption>
      <thead>
        <tr>
          <th v-for="coluna in colunas" :key="coluna.chave" scope="col">{{ coluna.titulo }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(registro, indice) in registros" :key="indice">
          <td v-for="coluna in colunas" :key="coluna.chave">
            <slot :name="'celula-' + coluna.chave" :registro="registro">{{
              valor(registro, coluna.chave)
            }}</slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
