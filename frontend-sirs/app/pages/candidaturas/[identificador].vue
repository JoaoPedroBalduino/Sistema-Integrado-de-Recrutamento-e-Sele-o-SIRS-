<script setup lang="ts">
import type { RespostaDetalheCandidatura } from '#shared/types/api'
import type { CampoFormulario } from '#shared/types/formulario'
import { nomesStatus, formatarNumero, formatarMoeda, formatarData } from '~/utils/formatacao'
definePageMeta({ perfis: ['ADMIN', 'COORD_RH', 'RECRUTADOR', 'GESTOR', 'AVALIADOR'] })
useHead({ title: 'Ficha da candidatura' })
const rota = useRoute()
const consulta = useApiSirs()
const autenticacao = useAutenticacao()
const caminho = computed(() => 'candidaturas/' + rota.params.identificador)
const { data, status, error, refresh } = await useAsyncData(
  () => caminho.value,
  () => consulta<RespostaDetalheCandidatura>(caminho.value),
)
const podeOperar = computed(
  () =>
    autenticacao.permite(['COORD_RH', 'RECRUTADOR', 'GESTOR']) &&
    !data.value?.candidatura.contratadaEm,
)
const podeAvaliar = computed(
  () =>
    (podeOperar.value || autenticacao.permite(['AVALIADOR'])) &&
    data.value?.candidatura.status === 'EM_AVALIACAO' &&
    !data.value?.candidatura.contratadaEm,
)
const operacao = ref('etapa')
const formulario = useFormulario(
  (valores) =>
    consulta(caminho.value + '/' + operacao.value, {
      method: ['etapa', 'status'].includes(operacao.value) ? 'PATCH' : 'POST',
      body: valores,
    }),
  refresh,
)
const campos = computed<CampoFormulario[]>(() =>
  operacao.value === 'etapa'
    ? [
        {
          chave: 'etapaId',
          rotulo: 'Próxima etapa',
          tipo: 'select',
          obrigatorio: true,
          opcoes:
            data.value?.etapas.map((etapa) => ({
              valor: etapa.identificador,
              rotulo: etapa.nome,
            })) || [],
        },
      ]
    : operacao.value === 'status'
      ? [
          {
            chave: 'status',
            rotulo: 'Decisão',
            tipo: 'select',
            obrigatorio: true,
            opcoes: [
              { valor: 'APROVADO', rotulo: 'Aprovar candidato' },
              { valor: 'REPROVADO', rotulo: 'Reprovar candidato' },
            ],
          },
        ]
      : operacao.value === 'proposta'
        ? [
            {
              chave: 'salarioOfertado',
              rotulo: 'Salário ofertado (R$)',
              tipo: 'number',
              obrigatorio: true,
              minimo: 0.01,
              passo: '0.01',
            },
            {
              chave: 'dataProposta',
              rotulo: 'Data da proposta',
              tipo: 'date',
              obrigatorio: true,
              valor: dataHoje(),
            },
          ]
        : operacao.value === 'contratacao'
          ? [
              {
                chave: 'dataAceite',
                rotulo: 'Data de aceite',
                tipo: 'date',
                obrigatorio: true,
                valor: dataHoje(),
              },
            ]
          : [
              {
                chave: 'usuarioId',
                rotulo: 'Identificador do avaliador',
                tipo: 'number',
                obrigatorio: true,
                minimo: 1,
                dica: 'Informe o identificador de uma conta com perfil Avaliador. Consulte Administração.',
              },
            ],
)
function abrir(tipo: string) {
  operacao.value = tipo
  formulario.abrir()
}
const notas = reactive<Record<number, number | null>>({})
const salvandoNotas = ref(false)
const erroNotas = ref('')
watch(
  data,
  (valor) => {
    if (valor)
      for (const criterio of valor.criterios)
        notas[criterio.identificador] =
          valor.notas.find((nota) => nota.criterioId === criterio.identificador)?.nota ?? null
  },
  { immediate: true },
)
async function avaliar() {
  salvandoNotas.value = true
  erroNotas.value = ''
  try {
    const valores = Object.entries(notas)
      .filter(([, nota]) => nota !== null && nota !== undefined && String(nota) !== '')
      .map(([criterioId, nota]) => ({ criterioId: Number(criterioId), nota: Number(nota) }))
    if (!valores.length) {
      erroNotas.value = 'Informe pelo menos uma nota.'
      return
    }
    await consulta(caminho.value + '/avaliacoes', { method: 'PUT', body: { notas: valores } })
    await refresh()
    useNotificacoes().adicionar('Avaliação registrada.')
  } catch (falha) {
    erroNotas.value = descreverErro(falha).mensagem
  } finally {
    salvandoNotas.value = false
  }
}
const avaliadorRemover = ref<number | null>(null)
async function removerAvaliador() {
  if (!avaliadorRemover.value) return
  try {
    await consulta(caminho.value + '/avaliadores/' + avaliadorRemover.value, { method: 'DELETE' })
    avaliadorRemover.value = null
    await refresh()
    useNotificacoes().adicionar('Designação removida.')
  } catch (falha) {
    useNotificacoes().adicionar(descreverErro(falha).mensagem, 'erro')
  }
}
</script>
<template>
  <NuxtLink to="/candidaturas" class="back-link"
    ><SirsIcone nome="back" :tamanho="15" />Todas as candidaturas</NuxtLink
  ><SirsEstado :carregando="status === 'pending'" :erro="error" @tentar="refresh"
    ><template v-if="data"
      ><SirsCabecalho
        :titulo="data.candidatura.nomeCandidato"
        :descricao="data.candidatura.tituloVaga"
        categoria="CANDIDATURA"
        ><SirsBadge :valor="data.candidatura.status" /><button
          v-if="podeOperar && data.candidatura.status === 'EM_AVALIACAO'"
          class="button secondary"
          @click="abrir('etapa')"
        >
          Mover etapa</button
        ><button
          v-if="podeOperar && data.candidatura.status === 'EM_AVALIACAO'"
          class="button primary"
          @click="abrir('status')"
        >
          Registrar decisão
        </button></SirsCabecalho
      >
      <div class="detail-grid">
        <div>
          <section class="panel">
            <div class="panel-header"><h2>Perfil do talento</h2></div>
            <div class="panel-body">
              <dl class="information-grid">
                <div>
                  <dt>E-mail</dt>
                  <dd>{{ data.candidato.email }}</dd>
                </div>
                <div>
                  <dt>Etapa atual</dt>
                  <dd>{{ data.candidatura.etapaAtual }}</dd>
                </div>
                <div>
                  <dt>Base legal</dt>
                  <dd>{{ nomesStatus[data.candidato.baseLegal] }}</dd>
                </div>
              </dl>
              <a
                v-if="data.candidato.curriculoUrl?.startsWith('https://')"
                :href="data.candidato.curriculoUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="button secondary"
                style="margin-top: 20px"
                ><SirsIcone nome="file" :tamanho="16" />Abrir currículo</a
              >
            </div>
          </section>
          <section class="panel">
            <div class="panel-header">
              <div>
                <h2>Avaliação por critérios</h2>
                <p>Notas de 0 a 5. A nota final é calculada pelo backend.</p>
              </div>
              <span class="score">{{ formatarNota(data.candidatura.notaFinal) }}</span>
            </div>
            <div class="panel-body">
              <p v-if="erroNotas" class="inline-alert" role="alert">{{ erroNotas }}</p>
              <form @submit.prevent="avaliar">
                <div
                  v-for="criterio in data.criterios"
                  :key="criterio.identificador"
                  class="list-item"
                >
                  <div>
                    <strong
                      >{{ criterio.nome }}
                      <span class="visually-muted">· Peso {{ criterio.peso }}</span></strong
                    >
                    <p>{{ criterio.descricao }}</p>
                  </div>
                  <label :for="'nota-' + criterio.identificador" class="sr-only"
                    >Nota para {{ criterio.nome }}</label
                  ><input
                    :id="'nota-' + criterio.identificador"
                    v-model.number="notas[criterio.identificador]"
                    type="number"
                    min="0"
                    max="5"
                    step="1"
                    :disabled="!podeAvaliar"
                    style="width: 75px"
                  />
                </div>
                <SirsEstado
                  v-if="!data.criterios.length"
                  vazio
                  titulo="Nenhum critério cadastrado"
                />
                <div v-if="podeAvaliar && data.criterios.length" class="form-footer">
                  <button class="button primary" :disabled="salvandoNotas">
                    {{ salvandoNotas ? 'Salvando…' : 'Salvar avaliação' }}
                  </button>
                </div>
              </form>
            </div>
          </section>
          <section v-if="autenticacao.usuario.value?.perfil !== 'AVALIADOR'" class="panel">
            <div class="panel-header">
              <h2>Proposta e contratação</h2>
              <button
                v-if="podeOperar && data.candidatura.status === 'APROVADO' && !data.proposta"
                class="button primary compact"
                @click="abrir('proposta')"
              >
                Registrar proposta
              </button>
            </div>
            <div class="panel-body">
              <template v-if="data.proposta"
                ><dl class="information-grid">
                  <div>
                    <dt>Salário ofertado</dt>
                    <dd>{{ formatarMoeda(data.proposta.salarioOfertado) }}</dd>
                  </div>
                  <div>
                    <dt>Proposta enviada em</dt>
                    <dd>{{ formatarData(data.proposta.dataProposta) }}</dd>
                  </div>
                  <div>
                    <dt>Data de aceite</dt>
                    <dd>{{ formatarData(data.proposta.dataAceite) }}</dd>
                  </div>
                </dl>
                <button
                  v-if="podeOperar && !data.candidatura.contratadaEm"
                  class="button primary"
                  style="margin-top: 20px"
                  @click="abrir('contratacao')"
                >
                  Confirmar contratação
                </button></template
              >
              <p v-else class="detail-note">
                A proposta pode ser registrada após aprovação na última etapa da vaga.
              </p>
            </div>
          </section>
        </div>
        <div>
          <section class="panel">
            <div class="panel-header"><h2>Jornada do candidato</h2></div>
            <div class="panel-body">
              <div v-for="(movimento, indice) in data.historico" :key="indice" class="list-item">
                <span><SirsIcone nome="check" :tamanho="15" /></span>
                <div>
                  <strong>{{
                    data.etapas.find((etapa) => etapa.identificador === movimento.etapaId)?.nome ||
                    'Etapa ' + movimento.etapaId
                  }}</strong>
                  <p>{{ formatarData(movimento.movidoEm) }}</p>
                </div>
              </div>
            </div>
          </section>
          <section class="panel">
            <div class="panel-header">
              <h2>Avaliadores</h2>
              <button
                v-if="podeOperar"
                class="icon-button"
                aria-label="Designar avaliador"
                @click="abrir('avaliadores')"
              >
                <SirsIcone nome="plus" :tamanho="16" />
              </button>
            </div>
            <div class="panel-body">
              <div v-for="avaliador in data.avaliadores" :key="avaliador" class="list-item">
                <div>
                  <strong>Avaliador #{{ avaliador }}</strong>
                </div>
                <button
                  v-if="podeOperar"
                  class="icon-button"
                  :aria-label="'Remover avaliador ' + avaliador"
                  @click="avaliadorRemover = avaliador"
                >
                  <SirsIcone nome="close" :tamanho="15" />
                </button>
              </div>
              <p v-if="!data.avaliadores.length" class="detail-note">Nenhum avaliador designado.</p>
            </div>
          </section>
        </div>
      </div></template
    ></SirsEstado
  ><SirsModal
    :aberto="formulario.aberto.value"
    :titulo="
      operacao === 'etapa'
        ? 'Mover candidatura'
        : operacao === 'status'
          ? 'Registrar decisão'
          : operacao === 'proposta'
            ? 'Proposta salarial'
            : operacao === 'contratacao'
              ? 'Confirmar contratação'
              : 'Designar avaliador'
    "
    @fechar="formulario.fechar"
    ><SirsFormulario
      :campos="campos"
      :salvando="formulario.salvando.value"
      :erro="formulario.erroFormulario.value"
      @salvar="formulario.salvar"
      @cancelar="formulario.fechar" /></SirsModal
  ><SirsModal
    :aberto="avaliadorRemover !== null"
    titulo="Remover avaliador?"
    @fechar="avaliadorRemover = null"
    ><p class="detail-note">A pessoa perderá a designação para esta candidatura.</p>
    <div class="form-footer">
      <button class="button secondary" @click="avaliadorRemover = null">Cancelar</button
      ><button class="button danger" @click="removerAvaliador">Remover designação</button>
    </div></SirsModal
  >
</template>
