<script setup lang="ts">
import type { RespostaAuditoria } from '#shared/types/api'
import { formatarData } from '~/utils/formatacao'
definePageMeta({ perfis: ['ADMIN', 'COORD_RH'] })
useHead({ title: 'Auditoria' })
const { data, status, error, refresh, pagina } = useColecao<RespostaAuditoria>('auditoria')
</script>
<template>
  <SirsCabecalho
    titulo="Auditoria"
    descricao="Acompanhe a rastreabilidade das operações, sem expor dados sensíveis."
    categoria="AUDITORIA"
  />
  <section class="panel">
    <div class="panel-header">
      <h2>Trilha de atividades</h2>
      <span class="badge">Somente leitura</span>
    </div>
    <SirsEstado
      :carregando="status === 'pending'"
      :erro="error"
      :vazio="!data?.content.length"
      titulo="Nenhuma atividade registrada"
      @tentar="refresh"
      ><SirsTabela
        :registros="data?.content || []"
        :colunas="[
          { chave: 'ocorridaEm', titulo: 'Data' },
          { chave: 'usuario_id', titulo: 'Responsável' },
          { chave: 'entidade', titulo: 'Recurso' },
          { chave: 'entidadeId', titulo: 'Identificador' },
          { chave: 'acao', titulo: 'Ação' },
        ]"
        legenda="Atividades auditadas"
        ><template #celula-ocorridaEm="{ registro }">{{
          formatarData(registro.ocorridaEm)
        }}</template
        ><template #celula-usuario_id="{ registro }"
          >Usuário #{{ registro.usuarioId }}</template
        ></SirsTabela
      ></SirsEstado
    ><SirsPaginacao
      v-if="data"
      :pagina="pagina"
      :total="data.totalElements"
      :total-paginas="data.totalPages"
      @mudar="pagina = $event"
    />
  </section>
</template>
