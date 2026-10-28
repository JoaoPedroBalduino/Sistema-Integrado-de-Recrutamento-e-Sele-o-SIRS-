<script setup lang="ts">
import { navegacao } from '~/utils/permissoes'
import { nomesPerfis, iniciais } from '~/utils/formatacao'
const autenticacao = useAutenticacao()
const notificacoes = useNotificacoes()
const menuAberto = ref(false)
const buscaAberta = ref(false)
const busca = ref('')
const rota = useRoute()
const links = computed(() =>
  navegacao.filter((item) => !item.perfis || autenticacao.permite(item.perfis)),
)
const linksBuscados = computed(() =>
  links.value.filter((item) =>
    item.titulo.toLocaleLowerCase('pt-BR').includes(busca.value.toLocaleLowerCase('pt-BR')),
  ),
)
const tituloAtual = computed(
  () =>
    links.value.find((item) => item.caminho !== '/' && rota.path.startsWith(item.caminho))
      ?.titulo || 'Visão geral',
)
watch(
  () => rota.path,
  () => {
    menuAberto.value = false
    buscaAberta.value = false
  },
)
async function sair() {
  try {
    await autenticacao.sair()
    notificacoes.adicionar('Você saiu da sua conta.', 'sucesso', 'Sessão encerrada')
  } catch {
    notificacoes.adicionar(
      'A sessão local foi encerrada. O serviço de autenticação está indisponível.',
      'erro',
    )
  }
}
</script>
<template>
  <div class="application-shell">
    <a class="skip-link" href="#conteudo-principal">Ir para o conteúdo</a>
    <div v-if="menuAberto" class="sidebar-overlay" @click="menuAberto = false" />
    <aside class="sidebar" :class="{ 'is-open': menuAberto }">
      <NuxtLink to="/" class="brand" aria-label="SIRS · Página inicial"><SirsMarca /></NuxtLink>
      <nav aria-label="Navegação principal">
        <template v-for="(item, indice) in links" :key="item.caminho"
          ><p v-if="indice === 0 || item.grupo !== links[indice - 1]?.grupo" class="nav-section">
            {{ item.grupo }}
          </p>
          <NuxtLink
            :to="item.caminho"
            class="nav-link"
            :class="{
              active: item.caminho === '/' ? rota.path === '/' : rota.path.startsWith(item.caminho),
            }"
            ><SirsIcone :nome="item.icone" />{{ item.titulo }}</NuxtLink
          ></template
        >
      </nav>
      <div class="sidebar-bottom">
        <NuxtLink to="/conta" class="user-card"
          ><span class="avatar">{{ iniciais(autenticacao.usuario.value?.nome || '') }}</span
          ><span class="user-information"
            ><strong>{{ autenticacao.usuario.value?.nome }}</strong
            ><small>{{ nomesPerfis[autenticacao.usuario.value?.perfil || ''] }}</small></span
          ><SirsIcone nome="settings" :tamanho="17"
        /></NuxtLink>
      </div>
    </aside>
    <div class="main-shell">
      <header class="topbar">
        <div class="breadcrumb">
          <button
            class="icon-button mobile-menu"
            aria-label="Abrir menu"
            :aria-expanded="menuAberto"
            @click="menuAberto = !menuAberto"
          >
            <SirsIcone nome="menu" /></button
          ><span class="breadcrumb-home">Equipe de recrutamento</span
          ><SirsIcone nome="chevron" :tamanho="14" /><strong>{{ tituloAtual }}</strong>
        </div>
        <div class="topbar-actions">
          <button class="search-trigger" @click="buscaAberta = true">
            <SirsIcone nome="search" :tamanho="17" /><span>Buscar página</span></button
          ><span class="topbar-divider" /><button
            class="icon-button"
            aria-label="Sair da conta"
            @click="sair"
          >
            <SirsIcone nome="logout" /></button
          ><NuxtLink to="/conta" class="avatar topbar-avatar" aria-label="Minha conta">{{
            iniciais(autenticacao.usuario.value?.nome || '')
          }}</NuxtLink>
        </div>
      </header>
      <main id="conteudo-principal" class="page-content"><slot /></main>
      <footer class="application-footer">
        <span>SIRS · Recrutamento e seleção</span><NuxtLink to="/conta">Minha conta</NuxtLink>
      </footer>
    </div>
    <SirsModal :aberto="buscaAberta" titulo="Ir para uma página" @fechar="buscaAberta = false"
      ><label class="sr-only" for="busca-navegacao">Buscar página</label
      ><input
        id="busca-navegacao"
        v-model="busca"
        type="search"
        placeholder="Digite o nome de uma página"
        class="wide-input"
      />
      <div class="command-results">
        <NuxtLink v-for="item in linksBuscados" :key="item.caminho" :to="item.caminho"
          ><SirsIcone :nome="item.icone" />{{ item.titulo }}<SirsIcone nome="chevron" :tamanho="16"
        /></NuxtLink>
        <p v-if="!linksBuscados.length">Nenhuma página encontrada.</p>
      </div></SirsModal
    >
  </div>
</template>
