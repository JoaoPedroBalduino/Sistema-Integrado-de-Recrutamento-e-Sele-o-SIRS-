<script setup lang="ts">
import type { RespostaDetalheVaga } from '#shared/types/api'
import type { CampoFormulario, ValoresFormulario } from '#shared/types/formulario'
import { perfisOperacionais } from '~/utils/permissoes'
import { podeAlterarVaga, proximosStatus } from '~/utils/vagas'
import { formatarData, formatarMoeda, nomesStatus } from '~/utils/formatacao'
definePageMeta({ perfis: perfisOperacionais })
useHead({ title: 'Detalhes da vaga' })
const rota = useRoute()
const consulta = useApiSirs()
const autenticacao = useAutenticacao()
const aba = ref('informacoes')
const caminho = computed(() => 'vagas/' + rota.params.identificador)
const { data, status, error, refresh } = await useAsyncData(
  () => caminho.value,
  () => consulta<RespostaDetalheVaga>(caminho.value),
)
const podeAlterar = computed(() =>
  Boolean(
    data.value &&
    autenticacao.usuario.value &&
    podeAlterarVaga(
      data.value.vaga,
      autenticacao.usuario.value.perfil,
      autenticacao.usuario.value.identificador,
    ),
  ),
)
const editavel = computed(() => podeAlterar.value && data.value?.vaga.status === 'RASCUNHO')
const estados = computed(() =>
  data.value && autenticacao.usuario.value
    ? proximosStatus(data.value.vaga.status, autenticacao.usuario.value.perfil, podeAlterar.value)
    : [],
)
const operacao = ref<'etapas' | 'criterios' | 'horas' | 'status'>('etapas')
const registroId = ref<number | null>(null)
const formulario = useFormulario(
  (valores) =>
    consulta(
      caminho.value + '/' + operacao.value + (registroId.value ? '/' + registroId.value : ''),
      {
        method: operacao.value === 'status' ? 'PATCH' : registroId.value ? 'PUT' : 'POST',
        body:
          operacao.value === 'status' && valores.status !== 'FECHADA'
            ? { status: valores.status, dataFechamento: null }
            : valores,
      },
    ),
  refresh,
)
const campos = computed<CampoFormulario[]>(() =>
  operacao.value === 'status'
    ? [
        {
          chave: 'status',
          rotulo: 'Próximo status',
          tipo: 'select',
          obrigatorio: true,
          opcoes: estados.value.map((estado) => ({ valor: estado, rotulo: nomesStatus[estado]! })),
        },
        {
          chave: 'dataFechamento',
          rotulo: 'Data de fechamento',
          tipo: 'date',
          valor: dataHoje(),
          dica: 'Usada somente ao fechar a vaga.',
        },
      ]
    : operacao.value === 'horas'
      ? [
          {
            chave: 'data',
            rotulo: 'Data do trabalho',
            tipo: 'date',
            obrigatorio: true,
            valor: dataHoje(),
          },
          {
            chave: 'horas',
            rotulo: 'Horas dedicadas',
            tipo: 'number',
            obrigatorio: true,
            minimo: 0.01,
            maximo: 24,
            passo: '0.01',
          },
          { chave: 'atividade', rotulo: 'Atividade', tipo: 'textarea', maximo: 200 },
        ]
      : [
          { chave: 'nome', rotulo: 'Nome', obrigatorio: true, maximo: 150 },
          ...(operacao.value === 'etapas'
            ? [
                {
                  chave: 'ordem',
                  rotulo: 'Ordem da etapa',
                  tipo: 'number' as const,
                  obrigatorio: true,
                  minimo: 1,
                  valor: (data.value?.etapas.length || 0) + 1,
                },
              ]
            : [
                {
                  chave: 'peso',
                  rotulo: 'Peso do critério',
                  tipo: 'number' as const,
                  obrigatorio: true,
                  minimo: 1,
                  maximo: 5,
                  valor: 1,
                },
              ]),
          { chave: 'descricao', rotulo: 'Descrição', tipo: 'textarea', maximo: 5000 },
        ],
)
function abrir(tipo: typeof operacao.value, registro?: { identificador: number }) {
  operacao.value = tipo
  registroId.value = registro?.identificador ?? null
  formulario.abrir(registro)
}
const editarAberto = ref(false)
const salvandoEdicao = ref(false)
const erroEdicao = shallowRef<unknown>(null)
async function salvarVaga(valores: ValoresFormulario) {
  salvandoEdicao.value = true
  erroEdicao.value = null
  try {
    await consulta(caminho.value, { method: 'PUT', body: valores })
    editarAberto.value = false
    await refresh()
    useNotificacoes().adicionar('Vaga atualizada.')
  } catch (falha) {
    erroEdicao.value = falha
  } finally {
    salvandoEdicao.value = false
  }
}
const exclusao = ref<{ tipo: 'etapas' | 'criterios'; identificador: number; nome: string } | null>(
  null,
)
const excluindo = ref(false)
async function excluir() {
  if (!exclusao.value) return
  excluindo.value = true
  try {
    await consulta(caminho.value + '/' + exclusao.value.tipo + '/' + exclusao.value.identificador, {
      method: 'DELETE',
    })
    exclusao.value = null
    await refresh()
    useNotificacoes().adicionar('Registro removido.')
  } catch (falha) {
    useNotificacoes().adicionar(descreverErro(falha).mensagem, 'erro')
  } finally {
    excluindo.value = false
  }
}
</script>
<template>
  <NuxtLink to="/vagas" class="back-link"
    ><SirsIcone nome="back" :tamanho="15" />Todas as vagas</NuxtLink
  ><SirsEstado :carregando="status === 'pending'" :erro="error" @tentar="refresh"
    ><template v-if="data"
      ><SirsCabecalho
        :titulo="data.vaga.titulo"
        :descricao="`${data.vaga.area.nome} · ${data.vaga.cargo.nome}`"
        categoria="DETALHES DA OPORTUNIDADE"
        ><SirsBadge :valor="data.vaga.status" /><button
          v-if="editavel"
          class="button secondary"
          @click="editarAberto = true"
        >
          Editar vaga</button
        ><button v-if="estados.length" class="button primary" @click="abrir('status')">
          Alterar status<SirsIcone nome="next" :tamanho="15" /></button
      ></SirsCabecalho>
      <div class="metrics-grid">
        <SirsMetrica
          titulo="Posições"
          :valor="data.vaga.posicoes"
          descricao="Oportunidades nesta vaga"
        /><SirsMetrica
          titulo="Candidaturas"
          :valor="data.vaga.candidatos"
          descricao="Talentos no processo seletivo"
        /><SirsMetrica
          titulo="Contratações"
          :valor="data.vaga.posicoesPreenchidas"
          descricao="Posições já preenchidas"
        /><SirsMetrica
          titulo="Prazo alvo"
          :valor="`${data.vaga.prazoAlvoDias} dias`"
          :descricao="'Abertura em ' + formatarData(data.vaga.dataAbertura)"
        />
      </div>
      <div class="section-tabs" role="tablist" aria-label="Detalhes da vaga">
        <button
          v-for="item in [
            { chave: 'informacoes', titulo: 'Informações' },
            { chave: 'etapas', titulo: 'Etapas' },
            { chave: 'criterios', titulo: 'Critérios' },
            { chave: 'horas', titulo: 'Horas' },
            { chave: 'historico', titulo: 'Histórico' },
          ]"
          :key="item.chave"
          class="tab-button"
          role="tab"
          :aria-selected="aba === item.chave"
          :class="{ active: aba === item.chave }"
          @click="aba = item.chave"
        >
          {{ item.titulo }}
        </button>
      </div>
      <section v-if="aba === 'informacoes'" class="panel">
        <div class="panel-header">
          <h2>Sobre esta oportunidade</h2>
          <NuxtLink
            v-if="autenticacao.permite(['ADMIN', 'COORD_RH', 'RECRUTADOR', 'DIRETORIA'])"
            :to="'/custos?vagaId=' + data.vaga.identificador"
            class="text-link"
            >Ver custos<SirsIcone nome="external" :tamanho="14"
          /></NuxtLink>
        </div>
        <div class="panel-body">
          <dl class="information-grid">
            <div>
              <dt>Modelo de trabalho</dt>
              <dd>{{ data.vaga.remoto ? 'Remoto' : 'Presencial' }}</dd>
            </div>
            <div>
              <dt>Localização</dt>
              <dd>
                {{
                  [data.vaga.cidade, data.vaga.estado].filter(Boolean).join(' · ') ||
                  'Não informada'
                }}
              </dd>
            </div>
            <div>
              <dt>Divulgação</dt>
              <dd>{{ formatarMoeda(data.vaga.investimentoDivulgacao) }}</dd>
            </div>
            <div>
              <dt>Canais</dt>
              <dd>
                {{ data.vaga.canais.map((canal) => canal.nome).join(', ') || 'Nenhum canal' }}
              </dd>
            </div>
            <div>
              <dt>Publicação</dt>
              <dd>{{ formatarData(data.vaga.dataPublicacao) }}</dd>
            </div>
            <div>
              <dt>Fechamento</dt>
              <dd>{{ formatarData(data.vaga.dataFechamento) }}</dd>
            </div>
          </dl>
          <p class="detail-note" style="margin-top: 25px">
            {{ data.vaga.observacoes || 'Nenhuma observação adicionada.' }}
          </p>
        </div>
      </section>
      <section v-if="aba === 'etapas' || aba === 'criterios'" class="panel">
        <div class="panel-header">
          <div>
            <h2>{{ aba === 'etapas' ? 'Etapas do processo' : 'Critérios de avaliação' }}</h2>
            <p>A estrutura pode ser alterada enquanto a vaga está em rascunho.</p>
          </div>
          <button
            v-if="editavel"
            class="button primary compact"
            @click="abrir(aba as 'etapas' | 'criterios')"
          >
            <SirsIcone nome="plus" :tamanho="14" />{{
              aba === 'etapas' ? 'Adicionar etapa' : 'Adicionar critério'
            }}
          </button>
        </div>
        <div class="panel-body">
          <template v-if="aba === 'etapas'"
            ><div v-for="etapa in data.etapas" :key="etapa.identificador" class="list-item">
              <span>{{ etapa.ordem }}</span>
              <div>
                <strong>{{ etapa.nome }}</strong>
                <p>{{ etapa.descricao }}</p>
              </div>
              <div v-if="editavel" class="actions">
                <button class="button secondary compact" @click="abrir('etapas', etapa)">
                  Editar</button
                ><button
                  class="button danger compact"
                  :aria-label="'Remover etapa ' + etapa.nome"
                  @click="
                    exclusao = {
                      tipo: 'etapas',
                      identificador: etapa.identificador,
                      nome: etapa.nome,
                    }
                  "
                >
                  Remover
                </button>
              </div>
            </div>
            <SirsEstado
              v-if="!data.etapas.length"
              vazio
              titulo="Defina o caminho do processo"
              descricao="Cadastre as etapas antes de enviar a vaga para aprovação." /></template
          ><template v-else
            ><div
              v-for="criterio in data.criterios"
              :key="criterio.identificador"
              class="list-item"
            >
              <span>{{ criterio.peso }}</span>
              <div>
                <strong>{{ criterio.nome }}</strong>
                <p>{{ criterio.descricao }} · Peso {{ criterio.peso }}</p>
              </div>
              <div v-if="editavel" class="actions">
                <button class="button secondary compact" @click="abrir('criterios', criterio)">
                  Editar</button
                ><button
                  class="button danger compact"
                  @click="
                    exclusao = {
                      tipo: 'criterios',
                      identificador: criterio.identificador,
                      nome: criterio.nome,
                    }
                  "
                >
                  Remover
                </button>
              </div>
            </div>
            <SirsEstado
              v-if="!data.criterios.length"
              vazio
              titulo="Como os talentos serão avaliados?"
              descricao="Adicione critérios com pesos de 1 a 5."
          /></template>
        </div>
      </section>
      <section v-if="aba === 'horas'" class="panel">
        <div class="panel-header">
          <h2>Horas dedicadas à vaga</h2>
          <button
            v-if="podeAlterar && data.vaga.status !== 'FECHADA'"
            class="button primary compact"
            @click="abrir('horas')"
          >
            Registrar horas
          </button>
        </div>
        <SirsTabela
          v-if="data.apontamentos.length"
          :registros="data.apontamentos"
          :colunas="[
            { chave: 'data', titulo: 'Data' },
            { chave: 'horas', titulo: 'Horas' },
            { chave: 'atividade', titulo: 'Atividade' },
          ]"
          legenda="Apontamentos de horas"
          ><template #celula-data="{ registro }">{{
            formatarData(registro.data)
          }}</template></SirsTabela
        ><SirsEstado v-else vazio titulo="Nenhum apontamento registrado" />
      </section>
      <section v-if="aba === 'historico'" class="panel">
        <div class="panel-header"><h2>Histórico da oportunidade</h2></div>
        <SirsTabela
          :registros="data.historico"
          :colunas="[
            { chave: 'statusNovo', titulo: 'Status' },
            { chave: 'alteradoEm', titulo: 'Data' },
          ]"
          legenda="Histórico da vaga"
          ><template #celula-statusNovo="{ registro }"
            ><SirsBadge :valor="registro.statusNovo" /></template
          ><template #celula-alteradoEm="{ registro }">{{
            formatarData(registro.alteradoEm)
          }}</template></SirsTabela
        >
      </section></template
    ></SirsEstado
  ><SirsModal
    :aberto="formulario.aberto.value"
    :titulo="
      operacao === 'status'
        ? 'Alterar status da vaga'
        : operacao === 'horas'
          ? 'Registrar horas'
          : operacao === 'etapas'
            ? 'Etapa do processo'
            : 'Critério de avaliação'
    "
    @fechar="formulario.fechar"
    ><SirsFormulario
      :campos="campos"
      :inicial="formulario.inicial.value"
      :salvando="formulario.salvando.value"
      :erro="formulario.erroFormulario.value"
      @salvar="formulario.salvar"
      @cancelar="formulario.fechar" /></SirsModal
  ><SirsModal :aberto="editarAberto" titulo="Editar vaga" @fechar="editarAberto = false"
    ><VagaFormulario
      v-if="editarAberto && data"
      :vaga="data.vaga"
      :salvando="salvandoEdicao"
      :erro="erroEdicao"
      @salvar="salvarVaga"
      @cancelar="editarAberto = false" /></SirsModal
  ><SirsModal :aberto="Boolean(exclusao)" titulo="Remover registro?" @fechar="exclusao = null"
    ><p class="detail-note">Deseja remover {{ exclusao?.nome }} da estrutura desta vaga?</p>
    <div class="form-footer">
      <button class="button secondary" @click="exclusao = null">Cancelar</button
      ><button class="button danger" :disabled="excluindo" @click="excluir">
        Confirmar remoção
      </button>
    </div></SirsModal
  >
</template>
