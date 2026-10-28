<script setup lang="ts">
import type {
  RespostaPaginada,
  RespostaCadastroApoio,
  RespostaConfiguracaoTemporal,
  RespostaVisitasCanal,
} from '#shared/types/api'
import type { CampoFormulario } from '#shared/types/formulario'
import { chavesParametroCusto, chavesMetas } from '#shared/constants/dominios'
import { perfisOperacionais } from '~/utils/permissoes'
import { formatarData, formatarNumero } from '~/utils/formatacao'
definePageMeta({ perfis: perfisOperacionais })
useHead({ title: 'Cadastros' })
const autenticacao = useAutenticacao()
const consulta = useApiSirs()
const listar = useReferencias()
const aba = ref('areas')
const pagina = ref(0)
const abas = computed(() => [
  { chave: 'areas', titulo: 'Departamentos' },
  { chave: 'cargos', titulo: 'Cargos' },
  { chave: 'canais', titulo: 'Canais' },
  ...(autenticacao.permite(['ADMIN', 'COORD_RH', 'RECRUTADOR', 'DIRETORIA'])
    ? [
        { chave: 'parametros-custo', titulo: 'Parâmetros de custo' },
        { chave: 'metas', titulo: 'Metas' },
        { chave: 'visitas', titulo: 'Visitas por canal' },
      ]
    : []),
])
const apoio = computed(() => ['areas', 'cargos', 'canais'].includes(aba.value))
const caminho = computed(() =>
  apoio.value
    ? 'cadastros/' + aba.value
    : aba.value === 'visitas'
      ? 'visitas-canais'
      : 'configuracoes/' + aba.value,
)
watch(aba, () => {
  pagina.value = 0
})
type Registro = RespostaCadastroApoio | RespostaConfiguracaoTemporal | RespostaVisitasCanal
const { data, status, error, refresh } = await useAsyncData(
  () => 'cadastro-' + caminho.value,
  () =>
    consulta<RespostaPaginada<Registro>>(caminho.value, {
      query: { page: pagina.value, size: 10 },
    }),
  { watch: [pagina] },
)
const canais = ref<RespostaCadastroApoio[]>([])
const podeSalvar = computed(() =>
  aba.value === 'visitas'
    ? autenticacao.permite(['COORD_RH', 'RECRUTADOR'])
    : autenticacao.permite(['ADMIN', 'COORD_RH']),
)
const formulario = useFormulario(
  (valores) =>
    consulta(
      caminho.value +
        (formulario.inicial.value?.identificador && apoio.value
          ? '/' + formulario.inicial.value.identificador
          : ''),
      {
        method:
          aba.value === 'visitas' || (formulario.inicial.value?.identificador && apoio.value)
            ? 'PUT'
            : 'POST',
        body: valores,
      },
    ),
  refresh,
)
async function abrir(registro?: Registro) {
  if (aba.value === 'visitas') {
    try {
      canais.value = await listar<RespostaCadastroApoio>('cadastros/canais')
    } catch (falha) {
      useNotificacoes().adicionar(descreverErro(falha).mensagem, 'erro')
      return
    }
  }
  formulario.abrir(registro)
}
const campos = computed<CampoFormulario[]>(() =>
  apoio.value
    ? [
        { chave: 'nome', rotulo: 'Nome', obrigatorio: true, maximo: 100 },
        { chave: 'ativo', rotulo: 'Cadastro ativo', tipo: 'checkbox', valor: true },
      ]
    : aba.value === 'visitas'
      ? [
          {
            chave: 'canalId',
            rotulo: 'Canal',
            tipo: 'select',
            obrigatorio: true,
            opcoes: canais.value
              .filter((canal) => canal.ativo)
              .map((canal) => ({ valor: canal.identificador, rotulo: canal.nome })),
          },
          {
            chave: 'periodo',
            rotulo: 'Primeiro dia do mês',
            tipo: 'date',
            obrigatorio: true,
            valor: dataHoje().slice(0, 8) + '01',
            dica: 'O período deve ser sempre o dia 01 do mês.',
          },
          {
            chave: 'visitas',
            rotulo: 'Número de visitas',
            tipo: 'number',
            obrigatorio: true,
            minimo: 0,
          },
        ]
      : [
          {
            chave: 'chave',
            rotulo: aba.value === 'metas' ? 'Indicador' : 'Parâmetro',
            tipo: 'select',
            obrigatorio: true,
            opcoes:
              aba.value === 'metas'
                ? chavesMetas
                : chavesParametroCusto.map((chave) => ({
                    valor: chave,
                    rotulo: chave.toLowerCase().replaceAll('_', ' '),
                  })),
          },
          {
            chave: 'valor',
            rotulo: 'Valor',
            tipo: 'number',
            obrigatorio: true,
            minimo: 0,
            passo: '0.0001',
          },
          {
            chave: 'vigenciaInicio',
            rotulo: 'Início da vigência',
            tipo: 'date',
            obrigatorio: true,
            valor: dataHoje(),
          },
          {
            chave: 'vigenciaFim',
            rotulo: 'Fim da vigência',
            tipo: 'date',
            dica: 'Deixe vazio para vigência aberta. Períodos não podem se sobrepor.',
          },
        ],
)
const encerrandoId = ref<number | null>(null)
const encerramento = useFormulario(
  (valores) =>
    consulta(caminho.value + '/' + encerrandoId.value + '/vigencia', {
      method: 'PATCH',
      body: valores,
    }),
  refresh,
)
const linhas = computed(
  () =>
    data.value?.content.map((registro) => ({
      identificador: registro.identificador,
      nome:
        'nome' in registro
          ? registro.nome
          : 'chave' in registro
            ? registro.chave.toLowerCase().replaceAll('_', ' ')
            : 'Canal #' + registro.canalId,
      detalhe:
        'valor' in registro
          ? formatarNumero(registro.valor, 4)
          : 'visitas' in registro
            ? registro.visitas + ' visitas'
            : registro.ativo
              ? 'Ativo'
              : 'Desativado',
      periodo:
        'vigenciaInicio' in registro
          ? formatarData(registro.vigenciaInicio) +
            ' — ' +
            (registro.vigenciaFim ? formatarData(registro.vigenciaFim) : 'Em aberto')
          : 'periodo' in registro
            ? formatarData(registro.periodo)
            : '—',
      original: registro,
    })) || [],
)
function abrirEncerramento(identificador: number) {
  encerrandoId.value = identificador
  encerramento.abrir()
}
</script>
<template>
  <SirsCabecalho
    titulo="Cadastros"
    descricao="Mantenha departamentos, cargos, canais e parâmetros atualizados."
    categoria="CADASTROS E PARÂMETROS"
    ><button v-if="podeSalvar" class="button primary" @click="abrir()">
      <SirsIcone nome="plus" :tamanho="16" />{{
        aba === 'visitas' ? 'Registrar visitas' : 'Novo registro'
      }}
    </button></SirsCabecalho
  >
  <div class="section-tabs">
    <button
      v-for="item in abas"
      :key="item.chave"
      class="tab-button"
      :class="{ active: aba === item.chave }"
      @click="aba = item.chave"
    >
      {{ item.titulo }}
    </button>
  </div>
  <section class="panel">
    <div class="panel-header">
      <h2>{{ abas.find((item) => item.chave === aba)?.titulo }}</h2>
      <span class="badge">{{ data?.totalElements || 0 }} registros</span>
    </div>
    <SirsEstado
      :carregando="status === 'pending'"
      :erro="error"
      :vazio="!linhas.length"
      @tentar="refresh"
      ><SirsTabela
        :registros="linhas"
        :colunas="[
          { chave: 'nome', titulo: 'Nome' },
          { chave: 'detalhe', titulo: apoio ? 'Situação' : 'Valor' },
          ...(!apoio ? [{ chave: 'periodo', titulo: 'Período' }] : []),
          { chave: 'acao', titulo: '' },
        ]"
        legenda="Cadastros do sistema"
        ><template #celula-nome="{ registro }"
          ><strong>{{ registro.nome }}</strong></template
        ><template #celula-acao="{ registro }"
          ><button
            v-if="podeSalvar && (apoio || aba === 'visitas')"
            class="button secondary compact"
            :aria-label="'Editar ' + registro.nome"
            @click="abrir(registro.original)"
          >
            Editar</button
          ><button
            v-if="
              podeSalvar &&
              !apoio &&
              aba !== 'visitas' &&
              'vigenciaFim' in registro.original &&
              !registro.original.vigenciaFim
            "
            class="button secondary compact"
            @click="abrirEncerramento(registro.identificador)"
          >
            Encerrar vigência
          </button></template
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
    :titulo="formulario.inicial.value ? 'Editar cadastro' : 'Novo registro'"
    @fechar="formulario.fechar"
    ><SirsFormulario
      :campos="campos"
      :inicial="formulario.inicial.value"
      :salvando="formulario.salvando.value"
      :erro="formulario.erroFormulario.value"
      @salvar="formulario.salvar"
      @cancelar="formulario.fechar" /></SirsModal
  ><SirsModal
    :aberto="encerramento.aberto.value"
    titulo="Encerrar vigência"
    @fechar="encerramento.fechar"
    ><SirsFormulario
      :campos="[
        { chave: 'vigenciaFim', rotulo: 'Último dia de vigência', tipo: 'date', obrigatorio: true },
      ]"
      :salvando="encerramento.salvando.value"
      :erro="encerramento.erroFormulario.value"
      @salvar="encerramento.salvar"
      @cancelar="encerramento.fechar"
  /></SirsModal>
</template>
