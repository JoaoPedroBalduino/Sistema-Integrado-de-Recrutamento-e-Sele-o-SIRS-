/** Contratos espelhados dos records e enums do backend SIRS. */

export type BaseLegal = 'CONSENTIMENTO' | 'LEGITIMO_INTERESSE' | 'EXECUCAO_CONTRATO'

export type ChaveParametroCusto =
  | 'SALARIO_GESTOR'
  | 'SALARIO_RECRUTADOR'
  | 'SALARIO_COLABORADOR'
  | 'SALARIO_DESIGNER'
  | 'FATOR_ENCARGOS'
  | 'HORAS_MENSAIS'
  | 'KIT_BOAS_VINDAS'
  | 'VERBA_DIVULGACAO'
  | 'OUTROS_INVESTIMENTOS'
  | 'CURRICULOS_TRIADOS'
  | 'CANDIDATOS_DESAFIO'
  | 'CANDIDATOS_ENTREVISTA'
  | 'CANDIDATOS_FIT_CULTURAL'
  | 'COLABORADORES_FIT_CULTURAL'
  | 'HORAS_BRIEFING_GESTOR'
  | 'HORAS_BRIEFING_RECRUTADOR'
  | 'HORAS_CADASTRO'
  | 'HORAS_CRIATIVO'
  | 'MINUTOS_TRIAGEM'
  | 'HORAS_DINAMICA'
  | 'PARTICIPANTES_DINAMICA'
  | 'HORAS_PREPARACAO_DINAMICA'
  | 'HORAS_DESAFIO'
  | 'HORAS_ENTREVISTA'
  | 'HORAS_FIT_CULTURAL'
  | 'HORAS_PROPOSTA'
  | 'HORAS_ORIENTACOES'

export type IndicadorMeta = 'TEMPO_FECHAMENTO' | 'VAGAS_NO_PRAZO' | 'CUSTO_VAGA_FECHADA'

export type Perfil = 'ADMIN' | 'COORD_RH' | 'RECRUTADOR' | 'GESTOR' | 'AVALIADOR' | 'DIRETORIA'

export type StatusCandidatura = 'EM_AVALIACAO' | 'APROVADO' | 'REPROVADO'

export type StatusVaga = 'RASCUNHO' | 'EM_APROVACAO' | 'PUBLICADA' | 'CONGELADA' | 'FECHADA'

export type TipoCadastroApoio = 'AREAS' | 'CARGOS' | 'CANAIS'

export type TipoConfiguracaoTemporal = 'PARAMETRO' | 'META'

export type TipoSolicitacao = 'ACESSO' | 'CORRECAO' | 'ANONIMIZACAO'

export interface RespostaApontamentoHoras {
  identificador: number
  usuarioId: number
  data: string
  horas: number
  atividade: string
}

export interface RespostaAuditoria {
  identificador: number
  usuarioId: number
  entidade: string
  entidadeId: number
  acao: string
  ocorridaEm: string
}

export interface RespostaAutenticacao {
  accessToken: string
  tokenType: string
  expiresAt: string
  user: RespostaUsuario
  refreshToken: string | null
}

export interface RespostaAvaliacaoCriterio {
  criterioId: number
  nota: number
  avaliadorId: number
}

export interface RespostaCadastroApoio {
  identificador: number
  nome: string
  ativo: boolean
}

export interface RespostaCandidato {
  identificador: number
  nome: string
  email: string
  curriculoUrl: string | null
  baseLegal: BaseLegal
  dataColeta: string
  versaoConsentimento: string
  anonimizadoEm: string | null
}

export interface RespostaCandidatura {
  identificador: number
  vagaId: number
  tituloVaga: string
  candidatoId: number
  nomeCandidato: string
  emailCandidato: string
  canalId: number
  etapaAtualId: number
  etapaAtual: string
  status: StatusCandidatura
  notaFinal: number | null
  contratadaEm: string | null
}

export interface RespostaConfiguracaoTemporal {
  identificador: number
  chave: string
  valor: number
  vigenciaInicio: string
  vigenciaFim: string | null
}

export interface RespostaCriterioVaga {
  identificador: number
  nome: string
  descricao: string
  peso: number
}

export interface RespostaDetalheCandidatura {
  etapas: RespostaEtapaVaga[]
  criterios: RespostaCriterioVaga[]
  candidatura: RespostaCandidatura
  candidato: RespostaCandidato
  notas: Array<RespostaAvaliacaoCriterio>
  historico: Array<RespostaMovimentacaoCandidatura>
  avaliadores: Array<number>
  proposta: RespostaProposta | null
}

export interface RespostaDetalheVaga {
  vaga: RespostaVaga
  etapas: Array<RespostaEtapaVaga>
  criterios: Array<RespostaCriterioVaga>
  historico: Array<RespostaHistoricoVaga>
  apontamentos: Array<RespostaApontamentoHoras>
}

export interface RespostaEstimativaCusto {
  etapas: Array<RespostaEtapaCusto>
  custoHoras: number
  investimentosDiretos: number
  total: number
}

export interface RespostaEtapaCusto {
  descricao: string
  horas: number
  custo: number
}

export interface RespostaEtapaVaga {
  identificador: number
  nome: string
  descricao: string
  ordem: number
}

export interface RespostaExportacaoTitular {
  candidato: RespostaCandidato
  candidaturas: Array<RespostaInscricaoTitular>
}

export interface RespostaHistoricoVaga {
  identificador: number
  statusAnterior: string
  statusNovo: string
  usuarioId: number
  alteradoEm: string
}

export interface RespostaIndicadorCanal {
  canalId: number
  nome: string
  visitas: number
  contratacoes: number
  percentualCanal: number
  percentualGeral: number
}

export interface RespostaIndicadorCargo {
  cargoId: number
  nome: string
  posicoesAbertas: number
  candidatos: number
  mediaDiasAberto: number
  investimentoMedio: number
}

export interface RespostaIndicadorCriterio {
  criterioId: number
  nome: string
  mediaNota: number
}

export interface RespostaIndicadorEtapa {
  etapaId: number
  nome: string
  quantidadeAtual: number
  passaram: number
  percentualConversao: number
}

export interface RespostaIndicadorRecrutador {
  usuarioId: number
  nome: string
  posicoesTrabalhadas: number
  posicoesPreenchidas: number
  percentualFechamento: number
}

export interface RespostaIndicadorVaga {
  vagaId: number
  diasFechamento: number
  candidatosPorPosicao: number
  aprovados: number
  mediaNotaFinal: number
  funil: Array<RespostaIndicadorEtapa>
  criterios: Array<RespostaIndicadorCriterio>
}

export interface RespostaInscricaoTitular {
  candidaturaId: number
  vagaId: number
  status: StatusCandidatura
  contratadaEm: string | null
}

export interface RespostaMovimentacaoCandidatura {
  etapaId: number
  usuarioId: number
  movidoEm: string
}

export interface RespostaPaginada<T> {
  content: Array<T>
  paginacao: number
  size: number
  totalElements: number
  totalPages: number
}

export interface RespostaProposta {
  identificador: number
  salarioOfertado: number
  dataProposta: string
  dataAceite: string | null
}

export interface RespostaResponsavelVaga {
  identificador: number
  nome: string
}

export interface RespostaResumoIndicadores {
  de: string
  ate: string
  vagasFechadas: number
  vagasConsideradas: number
  vagasAtrasadas: number
  horasPorVagaFechada: number
  percentualNoPrazo: number
  custoPorVagaFechada: number
  mediaDiasAdmissao: number
  metasVigentes: Record<string, number>
}

export interface RespostaSolicitacaoTitular {
  identificador: number
  candidatoId: number
  tipo: TipoSolicitacao
  status: string
  solicitadaEm: string
  prazo: string
  concluidaEm: string | null
}

export interface RespostaUsuario {
  identificador: number
  nome: string
  email: string
  perfil: Perfil
  ativo: boolean
  bloqueado: boolean
}

export interface RespostaVaga {
  identificador: number
  version: number
  titulo: string
  cargo: RespostaCadastroApoio
  area: RespostaCadastroApoio
  solicitanteId: number
  recrutadorId: number
  posicoes: number
  cidade: string | null
  estado: string | null
  remoto: boolean
  observacoes: string | null
  status: StatusVaga
  dataAbertura: string
  dataPublicacao: string | null
  dataFechamento: string | null
  prazoAlvoDias: number
  investimentoDivulgacao: number
  canais: Array<RespostaCadastroApoio>
  candidatos: number
  posicoesPreenchidas: number
  diasAberto: number | null
}

export interface RespostaVisitasCanal {
  identificador: number
  canalId: number
  periodo: string
  visitas: number
}
