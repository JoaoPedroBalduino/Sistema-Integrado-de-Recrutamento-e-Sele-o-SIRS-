export const formatarMoeda = (valor: number | null | undefined) =>
  valor == null
    ? '—'
    : new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor)
export const formatarNumero = (valor: number | null | undefined, casas = 0) =>
  valor == null
    ? '—'
    : new Intl.NumberFormat('pt-BR', { maximumFractionDigits: casas }).format(valor)
export function formatarData(valor: string | null | undefined) {
  if (!valor) return '—'
  const data = new Date(valor.length === 10 ? valor + 'T12:00:00Z' : valor)
  return Number.isNaN(data.getTime())
    ? '—'
    : new Intl.DateTimeFormat('pt-BR', { timeZone: 'America/Sao_Paulo' }).format(data)
}
export function dataHoje() {
  const partes = new Intl.DateTimeFormat('pt-BR', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date())
  return ['year', 'month', 'day']
    .map((tipo) => partes.find((parte) => parte.type === tipo)!.value)
    .join('-')
}
export function iniciais(nome: string) {
  return (
    nome
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .map((parte) => parte[0])
      .slice(0, 2)
      .join('')
      .toUpperCase() || 'S'
  )
}
export const nomesPerfis: Record<string, string> = {
  ADMIN: 'Administrador',
  COORD_RH: 'Coordenação de RH',
  RECRUTADOR: 'Recrutador',
  GESTOR: 'Gestor',
  AVALIADOR: 'Avaliador',
  DIRETORIA: 'Diretoria',
}
export const nomesStatus: Record<string, string> = {
  RASCUNHO: 'Rascunho',
  EM_APROVACAO: 'Em aprovação',
  PUBLICADA: 'Publicada',
  CONGELADA: 'Congelada',
  FECHADA: 'Fechada',
  EM_AVALIACAO: 'Em avaliação',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado',
  ABERTA: 'Aberta',
  CONCLUIDA: 'Concluída',
  CONSENTIMENTO: 'Consentimento',
  LEGITIMO_INTERESSE: 'Legítimo interesse',
  EXECUCAO_CONTRATO: 'Execução de contrato',
}

export const formatarNota = (valor: number | null | undefined) =>
  valor == null ? 'Sem nota' : formatarNumero(valor * 100, 1) + '%'
