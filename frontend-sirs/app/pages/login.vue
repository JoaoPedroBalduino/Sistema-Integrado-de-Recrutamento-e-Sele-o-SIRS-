<script setup lang="ts">
import { destinoInterno } from '#shared/utils/navegacao'
definePageMeta({ layout: 'publico' })
useHead({ title: 'Entrar' })
const email = ref('')
const senha = ref('')
const mostrarSenha = ref(false)
const entrando = ref(false)
const erro = ref('')
const autenticacao = useAutenticacao()
const rota = useRoute()
async function entrar() {
  entrando.value = true
  erro.value = ''
  try {
    await autenticacao.entrar(email.value, senha.value)
    await navigateTo(destinoInterno(rota.query.destino))
  } catch (falha) {
    erro.value = descreverErro(falha).mensagem
  } finally {
    entrando.value = false
    senha.value = ''
  }
}
</script>
<template>
  <div class="login-page">
    <section class="login-story">
      <div class="brand"><SirsMarca /></div>
      <div class="login-story-body">
        <span class="login-eyebrow"><span class="connection-dot" />GESTÃO DE RECRUTAMENTO</span>
        <h1>Recrutamento organizado.<br /><span>Decisões mais claras.</span></h1>
        <p>
          Organize seus processos, acompanhe talentos e transforme dados em decisões. Tudo em um só
          lugar.
        </p>
        <div class="login-feature">
          <span><SirsIcone nome="kanban" :tamanho="22" /></span>
          <div>
            <strong>Processos mais claros</strong>
            <p>Da abertura da vaga à contratação.</p>
          </div>
        </div>
      </div>
      <div class="login-story-footer">
        <span>Recrutamento e seleção</span><span>SIRS {{ new Date().getFullYear() }}</span>
      </div>
    </section>
    <section class="login-form-panel">
      <div class="login-form-inner">
        <span class="login-form-icon"><SirsIcone nome="shield" :tamanho="26" /></span>
        <p class="eyebrow">BEM-VINDO AO SIRS</p>
        <h2>Entre na sua conta</h2>
        <p class="login-subtitle">Entre com sua conta para continuar.</p>
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
              /><button
                type="button"
                :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'"
                @click="mostrarSenha = !mostrarSenha"
              >
                {{ mostrarSenha ? 'Ocultar' : 'Mostrar' }}
              </button>
            </div>
          </div>
          <button class="button primary login-submit" :disabled="entrando">
            <SirsIcone v-if="entrando" nome="loading" class="spin" :tamanho="18" />{{
              entrando ? 'Entrando…' : 'Entrar'
            }}<SirsIcone v-if="!entrando" nome="next" :tamanho="18" />
          </button>
        </form>
        <p class="login-help">
          <SirsIcone nome="help" :tamanho="16" />Problemas para acessar? Fale com o administrador.
        </p>
        <div class="login-secure"><SirsIcone nome="lock" :tamanho="14" />Sessão protegida</div>
      </div>
    </section>
  </div>
</template>
