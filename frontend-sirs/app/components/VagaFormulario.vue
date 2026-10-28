<script setup lang="ts">
import type {
  RespostaCadastroApoio,
  RespostaResponsavelVaga,
  RespostaVaga,
} from '#shared/types/api'
import type { CampoFormulario, ValoresFormulario } from '#shared/types/formulario'
const props = defineProps<{ vaga?: RespostaVaga; salvando?: boolean; erro?: unknown }>()
defineEmits<{ salvar: [valores: ValoresFormulario]; cancelar: [] }>()
const listar = useReferencias()
const {
  data: referencias,
  status,
  error,
  refresh,
} = await useAsyncData('referencias-vagas', async () => {
  const [areas, cargos, canais, solicitantes, recrutadores] = await Promise.all([
    listar<RespostaCadastroApoio>('cadastros/areas'),
    listar<RespostaCadastroApoio>('cadastros/cargos'),
    listar<RespostaCadastroApoio>('cadastros/canais'),
    listar<RespostaResponsavelVaga>('cadastros/solicitantes'),
    listar<RespostaResponsavelVaga>('cadastros/recrutadores'),
  ])
  return { areas, cargos, canais, solicitantes, recrutadores }
})
const opcoes = (lista: Array<{ identificador: number; nome: string; ativo?: boolean }> = []) =>
  lista
    .filter((item) => item.ativo !== false)
    .map((item) => ({ valor: item.identificador, rotulo: item.nome }))
const campos = computed<CampoFormulario[]>(() => [
  { chave: 'titulo', rotulo: 'Título da vaga', obrigatorio: true, maximo: 200 },
  {
    chave: 'cargoId',
    rotulo: 'Cargo',
    tipo: 'select',
    obrigatorio: true,
    opcoes: opcoes(referencias.value?.cargos),
  },
  {
    chave: 'areaId',
    rotulo: 'Departamento',
    tipo: 'select',
    obrigatorio: true,
    opcoes: opcoes(referencias.value?.areas),
  },
  {
    chave: 'solicitanteId',
    rotulo: 'Solicitante',
    tipo: 'select',
    obrigatorio: true,
    opcoes: opcoes(referencias.value?.solicitantes),
  },
  {
    chave: 'recrutadorId',
    rotulo: 'Recrutador responsável',
    tipo: 'select',
    obrigatorio: true,
    opcoes: opcoes(referencias.value?.recrutadores),
  },
  {
    chave: 'posicoes',
    rotulo: 'Número de posições',
    tipo: 'number',
    obrigatorio: true,
    minimo: 1,
    valor: 1,
  },
  { chave: 'cidade', rotulo: 'Cidade', maximo: 100 },
  {
    chave: 'estado',
    rotulo: 'Estado',
    tipo: 'select',
    opcoes: 'AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO'
      .split(' ')
      .map((estado) => ({ valor: estado, rotulo: estado })),
  },
  { chave: 'remoto', rotulo: 'Trabalho remoto', tipo: 'checkbox' },
  {
    chave: 'dataAbertura',
    rotulo: 'Data de abertura',
    tipo: 'date',
    obrigatorio: true,
    valor: dataHoje(),
  },
  {
    chave: 'prazoAlvoDias',
    rotulo: 'Prazo alvo em dias',
    tipo: 'number',
    obrigatorio: true,
    minimo: 1,
    valor: 30,
  },
  {
    chave: 'investimentoDivulgacao',
    rotulo: 'Investimento em divulgação (R$)',
    tipo: 'number',
    obrigatorio: true,
    minimo: 0,
    passo: '0.01',
    valor: 0,
  },
  {
    chave: 'canalIds',
    rotulo: 'Canais de divulgação',
    tipo: 'multiple',
    opcoes: opcoes(referencias.value?.canais),
    dica: 'Segure Ctrl ou ⌘ para selecionar mais de um canal. Obrigatórios para publicar.',
  },
  { chave: 'observacoes', rotulo: 'Descrição e observações', tipo: 'textarea', maximo: 10000 },
])
const inicial = computed(() =>
  props.vaga
    ? {
        ...props.vaga,
        cargoId: props.vaga.cargo.identificador,
        areaId: props.vaga.area.identificador,
        canalIds: props.vaga.canais.map((canal) => canal.identificador),
      }
    : undefined,
)
</script>
<template>
  <SirsEstado :carregando="status === 'pending'" :erro="error" @tentar="refresh"
    ><div
      v-if="
        !referencias?.areas.length ||
        !referencias?.cargos.length ||
        !referencias?.recrutadores.length ||
        !referencias?.solicitantes.length
      "
      class="inline-note"
    >
      Antes de criar a vaga, cadastre departamento, cargo, solicitante e recrutador responsável. As
      opções respeitam os perfis ativos do sistema.
    </div>
    <SirsFormulario
      :campos="campos"
      :inicial="inicial"
      :salvando="salvando"
      :erro="erro"
      rotulo="Salvar vaga"
      @salvar="$emit('salvar', $event)"
      @cancelar="$emit('cancelar')"
  /></SirsEstado>
</template>
