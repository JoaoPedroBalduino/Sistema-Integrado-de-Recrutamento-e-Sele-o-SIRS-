<script setup lang="ts">
import type { RespostaSolicitacaoTitular, RespostaCandidato } from '#shared/types/api'
import type { CampoFormulario } from '#shared/types/formulario'
import { formatarData } from '~/utils/formatacao'
import { baixarArquivo } from '~/utils/exportacao'
definePageMeta({ perfis: ['ADMIN', 'COORD_RH'] })
useHead({ title: 'Privacidade' })
const autenticacao = useAutenticacao()
const consulta = useApiSirs()
const listar = useReferencias()
const { data, status, error, refresh, pagina } = useColecao<RespostaSolicitacaoTitular>(
  'privacidade/solicitacoes',
)
const candidatos = ref<RespostaCandidato[]>([])
const formulario = useFormulario(
  (valores) => consulta('privacidade/solicitacoes', { method: 'POST', body: valores }),
  refresh,
)
async function abrir() {
  try {
    candidatos.value = await listar<RespostaCandidato>('candidatos')
    formulario.abrir()
  } catch (falha) {
    useNotificacoes().adicionar(descreverErro(falha).mensagem, 'erro')
  }
}
const campos = computed<CampoFormulario[]>(() => [
  {
    chave: 'candidatoId',
    rotulo: 'Titular',
    tipo: 'select',
    obrigatorio: true,
    opcoes: candidatos.value.map((candidato) => ({
      valor: candidato.identificador,
      rotulo: candidato.nome + ' · ' + candidato.email,
    })),
  },
  {
    chave: 'tipo',
    rotulo: 'Tipo da solicitação',
    tipo: 'select',
    obrigatorio: true,
    opcoes: [
      { valor: 'ACESSO', rotulo: 'Acesso aos dados' },
      { valor: 'CORRECAO', rotulo: 'Correção cadastral' },
      { valor: 'ANONIMIZACAO', rotulo: 'Anonimização' },
    ],
  },
])
const conclusao = ref<RespostaSolicitacaoTitular | null>(null)
const concluindo = ref(false)
async function concluir() {
  if (!conclusao.value) return
  concluindo.value = true
  try {
    await consulta('privacidade/solicitacoes/' + conclusao.value.identificador + '/conclusao', {
      method: 'POST',
      body: {},
    })
    conclusao.value = null
    await refresh()
    useNotificacoes().adicionar('Solicitação concluída.')
  } catch (falha) {
    useNotificacoes().adicionar(descreverErro(falha).mensagem, 'erro')
  } finally {
    concluindo.value = false
  }
}
async function exportar(candidato: number) {
  try {
    const dados = await consulta('privacidade/candidatos/' + candidato + '/exportacao')
    baixarArquivo(
      'dados-titular-' + candidato + '.json',
      JSON.stringify(dados, null, 2),
      'application/json',
    )
    useNotificacoes().adicionar(
      'Exportação preparada para o titular.',
      'informacao',
      'Exportação de dados',
    )
  } catch (falha) {
    useNotificacoes().adicionar(descreverErro(falha).mensagem, 'erro')
  }
}
</script>
<template>
  <SirsCabecalho
    titulo="Privacidade"
    descricao="Gerencie as solicitações dos titulares e acompanhe seus prazos."
    categoria="PRIVACIDADE E LGPD"
    ><button v-if="autenticacao.permite(['ADMIN'])" class="button primary" @click="abrir">
      <SirsIcone nome="plus" :tamanho="16" />Nova solicitação
    </button></SirsCabecalho
  >
  <p class="inline-note">
    Correções cadastrais devem ser feitas antes da conclusão. A anonimização remove dados pessoais e
    só pode ser concluída quando as regras de retenção permitirem.
  </p>
  <section class="panel">
    <div class="panel-header">
      <h2>Solicitações de titulares</h2>
      <SirsIcone nome="shield" :tamanho="19" />
    </div>
    <SirsEstado
      :carregando="status === 'pending'"
      :erro="error"
      :vazio="!data?.content.length"
      titulo="Nenhuma solicitação pendente"
      @tentar="refresh"
      ><SirsTabela
        :registros="data?.content || []"
        :colunas="[
          { chave: 'candidatoId', titulo: 'Titular' },
          { chave: 'tipo', titulo: 'Solicitação' },
          { chave: 'prazo', titulo: 'Prazo' },
          { chave: 'status', titulo: 'Situação' },
          { chave: 'acao', titulo: '' },
        ]"
        legenda="Solicitações de privacidade"
        ><template #celula-candidatoId="{ registro }"
          >Candidato #{{ registro.candidatoId }}</template
        ><template #celula-tipo="{ registro }">{{
          registro.tipo === 'ACESSO'
            ? 'Acesso aos dados'
            : registro.tipo === 'CORRECAO'
              ? 'Correção'
              : 'Anonimização'
        }}</template
        ><template #celula-prazo="{ registro }">{{ formatarData(registro.prazo) }}</template
        ><template #celula-status="{ registro }"><SirsBadge :valor="registro.status" /></template
        ><template #celula-acao="{ registro }"
          ><div v-if="autenticacao.permite(['ADMIN'])" class="row-actions">
            <button
              v-if="registro.tipo === 'ACESSO'"
              class="button secondary compact"
              @click="exportar(registro.candidatoId)"
            >
              Exportar dados</button
            ><button
              v-if="registro.status === 'ABERTA'"
              class="button secondary compact"
              @click="conclusao = registro"
            >
              Concluir
            </button>
          </div></template
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
  <SirsModal
    :aberto="formulario.aberto.value"
    titulo="Solicitação do titular"
    @fechar="formulario.fechar"
    ><SirsFormulario
      :campos="campos"
      :salvando="formulario.salvando.value"
      :erro="formulario.erroFormulario.value"
      @salvar="formulario.salvar"
      @cancelar="formulario.fechar" /></SirsModal
  ><SirsModal :aberto="Boolean(conclusao)" titulo="Concluir solicitação?" @fechar="conclusao = null"
    ><p class="detail-note">
      {{
        conclusao?.tipo === 'ANONIMIZACAO'
          ? 'Esta ação remove os dados pessoais do titular, conforme as regras de retenção. Confirme somente após verificar a solicitação.'
          : 'Confirme que a solicitação do titular foi atendida. Esta ação será registrada na auditoria.'
      }}
    </p>
    <div class="form-footer">
      <button class="button secondary" :disabled="concluindo" @click="conclusao = null">
        Cancelar</button
      ><button class="button primary" :disabled="concluindo" @click="concluir">
        Confirmar conclusão
      </button>
    </div></SirsModal
  >
</template>
