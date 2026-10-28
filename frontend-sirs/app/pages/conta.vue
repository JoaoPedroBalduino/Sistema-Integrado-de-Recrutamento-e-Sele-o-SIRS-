<script setup lang="ts">
import type { CampoFormulario, ValoresFormulario } from '#shared/types/formulario'
import { nomesPerfis } from '~/utils/formatacao'
useHead({ title: 'Minha conta' })
const autenticacao = useAutenticacao()
const salvando = ref(false)
const erro = shallowRef<unknown>(null)
function explicarAcesso() {
  useNotificacoes().adicionar(
    'Seu perfil define as páginas e ações disponíveis. Para alterar suas permissões, fale com o administrador da equipe. A troca de senha encerra as sessões abertas da sua conta.',
    'informacao',
    'Sobre o seu acesso',
  )
}
const campos: CampoFormulario[] = [
  { chave: 'senhaAtual', rotulo: 'Senha atual', tipo: 'password', obrigatorio: true, maximo: 72 },
  {
    chave: 'novaSenha',
    rotulo: 'Nova senha',
    tipo: 'password',
    obrigatorio: true,
    minimo: 12,
    maximo: 72,
  },
  {
    chave: 'confirmacao',
    rotulo: 'Confirme a nova senha',
    tipo: 'password',
    obrigatorio: true,
    minimo: 12,
    maximo: 72,
  },
]
async function salvar(valores: ValoresFormulario) {
  if (valores.novaSenha !== valores.confirmacao) {
    erro.value = { data: { detail: 'A confirmação da nova senha não coincide.' } }
    useNotificacoes().adicionar(
      descreverErro(erro.value).mensagem,
      'alerta',
      'Confira a confirmação da senha',
    )
    return
  }
  salvando.value = true
  erro.value = null
  try {
    await $fetch('/api/auth/password', {
      method: 'PUT',
      body: { senhaAtual: valores.senhaAtual, novaSenha: valores.novaSenha },
    })
    autenticacao.usuario.value = null
    useNotificacoes().adicionar('Senha atualizada. Entre novamente.')
    await navigateTo('/login')
  } catch (falha) {
    erro.value = falha
    useNotificacoes().adicionar(descreverErro(falha).mensagem, 'erro')
  } finally {
    salvando.value = false
  }
}
</script>
<template>
  <SirsCabecalho
    titulo="Minha conta"
    descricao="Seu perfil e a segurança do seu acesso."
    categoria="ESPAÇO PESSOAL"
  />
  <section class="panel">
    <div class="panel-header">
      <h2>Sobre o seu acesso</h2>
      <div class="row-actions">
        <SirsBadge :valor="autenticacao.usuario.value?.perfil || ''" /><button
          class="button secondary compact"
          type="button"
          @click="explicarAcesso"
        >
          <SirsIcone nome="info" :tamanho="15" />Entenda seu acesso
        </button>
      </div>
    </div>
    <div class="panel-body">
      <dl class="information-grid">
        <div>
          <dt>Nome</dt>
          <dd>{{ autenticacao.usuario.value?.nome }}</dd>
        </div>
        <div>
          <dt>E-mail</dt>
          <dd>{{ autenticacao.usuario.value?.email }}</dd>
        </div>
        <div>
          <dt>Perfil</dt>
          <dd>{{ nomesPerfis[autenticacao.usuario.value?.perfil || ''] }}</dd>
        </div>
      </dl>
    </div>
  </section>
  <section class="panel">
    <div class="panel-header">
      <div>
        <h2>Atualizar senha</h2>
        <p>A alteração encerra todas as sessões da sua conta.</p>
      </div>
      <SirsIcone nome="lock" />
    </div>
    <div class="panel-body">
      <SirsFormulario
        :campos="campos"
        :salvando="salvando"
        :erro="erro"
        rotulo="Atualizar minha senha"
        @salvar="salvar"
        @cancelar="navigateTo('/')"
      />
    </div>
  </section>
</template>
