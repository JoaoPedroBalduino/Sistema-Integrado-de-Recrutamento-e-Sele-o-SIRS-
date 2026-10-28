<script setup lang="ts">
import { destinoInterno } from '#shared/utils/navegacao'
definePageMeta({ layout: 'publico' })
useHead({ title: 'Entrar' })
const email = ref('')
const senha = ref('')
const mostrarSenha = ref(false)
const maiusculasAtivadas = ref(false)
const entrando = ref(false)
const erro = ref('')
const autenticacao = useAutenticacao()
const rota = useRoute()
onMounted(() => {
  if (rota.query.expirada)
    useNotificacoes().adicionar(
      'Sua sessão expirou. Entre novamente para continuar.',
      'alerta',
      'Sessão expirada',
    )
})
function verificarMaiusculas(evento: KeyboardEvent) {
  maiusculasAtivadas.value = evento.getModifierState('CapsLock')
}
async function entrar() {
  entrando.value = true
  erro.value = ''
  try {
    await autenticacao.entrar(email.value, senha.value)
    await navigateTo(destinoInterno(rota.query.destino))
  } catch (falha) {
    erro.value = descreverErro(falha).mensagem
    useNotificacoes().adicionar(erro.value, 'erro', 'Não foi possível entrar')
  } finally {
    entrando.value = false
    senha.value = ''
  }
}
</script>
<template>
  <div class="login-page">
    <header class="login-header">
      <div class="brand"><SirsMarca /></div>
      <span>Acesso da equipe</span>
    </header>
    <main class="login-main">
      <section class="login-form-panel" aria-labelledby="titulo-login">
        <div class="login-form-inner">
          <p class="login-access-label"><SirsIcone nome="lock" :tamanho="14" />Acesso ao SIRS</p>
          <h1 id="titulo-login">Entre na sua conta</h1>
          <p class="login-subtitle">Use o e-mail cadastrado pela sua equipe.</p>
          <div v-if="rota.query.expirada" class="inline-alert">
            Sua sessão expirou. Entre novamente.
          </div>
          <div v-if="erro" class="inline-alert" role="alert">{{ erro }}</div>
          <form @submit.prevent="entrar">
            <div class="field">
              <label for="email">E-mail corporativo</label
              ><input
                id="email"
                v-model="email"
                type="email"
                autocomplete="username"
                inputmode="email"
                :spellcheck="false"
                autocapitalize="none"
                required
                maxlength="255"
                placeholder="voce@empresa.com.br"
              />
            </div>
            <div class="field">
              <label for="senha">Senha</label>
              <div class="password-input">
                <input
                  id="senha"
                  v-model="senha"
                  :type="mostrarSenha ? 'text' : 'password'"
                  autocomplete="current-password"
                  required
                  maxlength="72"
                  placeholder="Digite sua senha"
                  :aria-describedby="maiusculasAtivadas ? 'aviso-maiusculas' : undefined"
                  @keydown="verificarMaiusculas"
                  @keyup="verificarMaiusculas"
                  @blur="maiusculasAtivadas = false"
                /><button
                  type="button"
                  :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'"
                  :aria-pressed="mostrarSenha"
                  @click="mostrarSenha = !mostrarSenha"
                >
                  <SirsIcone :nome="mostrarSenha ? 'eyeOff' : 'eye'" :tamanho="18" />
                </button>
              </div>
            </div>
            <p
              v-if="maiusculasAtivadas"
              id="aviso-maiusculas"
              class="login-caps-lock"
              role="status"
            >
              <SirsIcone nome="alert" :tamanho="14" />Caps Lock ativado
            </p>
            <button type="submit" class="button primary login-submit" :disabled="entrando">
              <SirsIcone v-if="entrando" nome="loading" class="spin" :tamanho="18" />{{
                entrando ? 'Entrando…' : 'Entrar'
              }}<SirsIcone v-if="!entrando" nome="next" :tamanho="18" />
            </button>
          </form>

          <p class="login-help">Precisa de acesso? Fale com o administrador da sua equipe.</p>
        </div>
      </section>
      <aside class="login-context" aria-labelledby="titulo-processo">
        <div class="login-context-heading">
          <span class="login-context-rule" />
          <p class="login-section-label">O processo, de ponta a ponta</p>
        </div>
        <h2 id="titulo-processo">Da vaga<br />à admissão<span>.</span></h2>
        <p class="login-context-intro">Um processo por vaga. Cada candidatura com seu histórico.</p>
        <ol class="login-process" aria-label="Etapas de um processo seletivo">
          <li>
            <span class="login-step-marker"><SirsIcone nome="briefcase" :tamanho="18" /></span>
            <div>
              <h3>Defina a vaga</h3>
              <p>Requisitos, responsáveis e prazos.</p>
            </div>
            <span class="login-step-number">01</span>
          </li>
          <li>
            <span class="login-step-marker"><SirsIcone nome="users" :tamanho="18" /></span>
            <div>
              <h3>Acompanhe as candidaturas</h3>
              <p>Entrevistas e avaliações por etapa.</p>
            </div>
            <span class="login-step-number">02</span>
          </li>
          <li>
            <span class="login-step-marker"><SirsIcone nome="check" :tamanho="18" /></span>
            <div>
              <h3>Registre a contratação</h3>
              <p>Proposta, admissão e custos.</p>
            </div>
            <span class="login-step-number">03</span>
          </li>
        </ol>
        <p class="login-context-note">
          Seu perfil define as informações e ações disponíveis no sistema.
        </p>
      </aside>
    </main>
    <footer class="login-footer">
      <span>SIRS · Recrutamento e seleção</span><span>{{ new Date().getFullYear() }}</span>
    </footer>
  </div>
</template>
