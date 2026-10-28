<script setup lang="ts">
import type { RespostaUsuario } from '#shared/types/api'
import type { CampoFormulario } from '#shared/types/formulario'
import { perfisAdministracao } from '~/utils/permissoes'
import { nomesPerfis, iniciais } from '~/utils/formatacao'
definePageMeta({ perfis: perfisAdministracao })
useHead({ title: 'Administração' })
const consulta = useApiSirs()
const autenticacao = useAutenticacao()
const { data, status, error, refresh, pagina } = useColecao<RespostaUsuario>('usuarios')
const formulario = useFormulario(
  (valores) =>
    consulta(
      'usuarios' +
        (formulario.inicial.value?.identificador
          ? '/' + formulario.inicial.value.identificador
          : ''),
      { method: formulario.inicial.value?.identificador ? 'PUT' : 'POST', body: valores },
    ),
  refresh,
)
const campos = computed<CampoFormulario[]>(() => [
  { chave: 'nome', rotulo: 'Nome completo', obrigatorio: true, maximo: 150 },
  ...(!formulario.inicial.value?.identificador
    ? [
        {
          chave: 'email',
          rotulo: 'E-mail corporativo',
          tipo: 'email' as const,
          obrigatorio: true,
          maximo: 255,
        },
        {
          chave: 'senha',
          rotulo: 'Senha inicial',
          tipo: 'password' as const,
          obrigatorio: true,
          minimo: 12,
          maximo: 72,
          dica: 'Mínimo de 12 caracteres e máximo de 72 bytes UTF-8.',
        },
      ]
    : []),
  {
    chave: 'perfil',
    rotulo: 'Perfil de acesso',
    tipo: 'select',
    obrigatorio: true,
    opcoes: Object.entries(nomesPerfis).map(([valor, rotulo]) => ({ valor, rotulo })),
  },
  ...(formulario.inicial.value?.identificador
    ? [
        { chave: 'ativo', rotulo: 'Conta ativa', tipo: 'checkbox' as const },
        { chave: 'bloqueado', rotulo: 'Conta bloqueada', tipo: 'checkbox' as const },
      ]
    : []),
])
const usuarioVinculo = ref<RespostaUsuario | null>(null)
const vinculo = useFormulario(
  (valores) =>
    consulta('usuarios/' + usuarioVinculo.value!.identificador + '/supabase', {
      method: 'PUT',
      body: valores,
    }),
  refresh,
)
const colunas = [
  { chave: 'nome', titulo: 'Pessoa' },
  { chave: 'perfil', titulo: 'Perfil' },
  { chave: 'situacao', titulo: 'Situação' },
  { chave: 'acao', titulo: '' },
]
function abrirVinculo(usuario: RespostaUsuario) {
  usuarioVinculo.value = usuario
  vinculo.abrir()
}
</script>
<template>
  <SirsCabecalho
    titulo="Usuários e perfis"
    descricao="Gerencie contas e permissões de acesso da equipe."
    categoria="ADMINISTRAÇÃO"
    ><button
      v-if="autenticacao.permite(['ADMIN'])"
      class="button primary"
      @click="formulario.abrir()"
    >
      <SirsIcone nome="plus" :tamanho="16" />Novo usuário
    </button></SirsCabecalho
  >
  <div class="inline-note">
    <strong>O perfil define o acesso, não o cargo.</strong> Alterações de perfil, bloqueio ou
    desativação encerram as sessões anteriores da conta.
  </div>
  <section class="panel">
    <div class="panel-header">
      <h2>Equipe e permissões</h2>
      <span class="badge">{{ data?.totalElements || 0 }} usuários</span>
    </div>
    <SirsEstado
      :carregando="status === 'pending'"
      :erro="error"
      :vazio="!data?.content.length"
      @tentar="refresh"
      ><SirsTabela
        :registros="data?.content || []"
        :colunas="colunas"
        legenda="Usuários e permissões"
        ><template #celula-nome="{ registro }"
          ><div class="table-person">
            <span class="avatar">{{ iniciais(registro.nome) }}</span>
            <div>
              <strong>{{ registro.nome }}</strong
              ><small>{{ registro.email }} · #{{ registro.identificador }}</small>
            </div>
          </div></template
        ><template #celula-perfil="{ registro }"><SirsBadge :valor="registro.perfil" /></template
        ><template #celula-situacao="{ registro }"
          ><span
            class="badge"
            :data-status="registro.ativo && !registro.bloqueado ? 'PUBLICADA' : 'REPROVADO'"
            ><span class="badge-dot" />{{
              !registro.ativo ? 'Desativado' : registro.bloqueado ? 'Bloqueado' : 'Ativo'
            }}</span
          ></template
        ><template #celula-acao="{ registro }"
          ><div v-if="autenticacao.permite(['ADMIN'])" class="row-actions">
            <button
              class="button secondary compact"
              :aria-label="'Editar usuário ' + registro.nome"
              @click="formulario.abrir(registro)"
            >
              Editar</button
            ><button
              class="icon-button"
              :aria-label="'Vincular Supabase para ' + registro.nome"
              @click="abrirVinculo(registro)"
            >
              <SirsIcone nome="lock" :tamanho="15" />
            </button></div></template></SirsTabela></SirsEstado
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
    :titulo="
      formulario.inicial.value?.identificador ? 'Editar acesso' : 'Adicionar pessoa à equipe'
    "
    @fechar="formulario.fechar"
    ><SirsFormulario
      :campos="campos"
      :inicial="formulario.inicial.value"
      :salvando="formulario.salvando.value"
      :erro="formulario.erroFormulario.value"
      @salvar="formulario.salvar"
      @cancelar="formulario.fechar" /></SirsModal
  ><SirsModal
    :aberto="vinculo.aberto.value"
    titulo="Vincular identidade Supabase"
    @fechar="vinculo.fechar"
    ><p class="inline-note">
      Vincule apenas a conta cujo e-mail coincide com {{ usuarioVinculo?.email }}. As sessões
      anteriores serão revogadas.
    </p>
    <SirsFormulario
      :campos="[
        {
          chave: 'usuarioSupabaseId',
          rotulo: 'UUID do usuário no Supabase',
          obrigatorio: true,
          maximo: 36,
        },
      ]"
      :salvando="vinculo.salvando.value"
      :erro="vinculo.erroFormulario.value"
      @salvar="vinculo.salvar"
      @cancelar="vinculo.fechar"
  /></SirsModal>
</template>
