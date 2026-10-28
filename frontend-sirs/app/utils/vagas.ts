import type { Perfil, StatusVaga, RespostaVaga } from '#shared/types/api'
export function podeAlterarVaga(vaga: RespostaVaga, perfil: Perfil, usuario: number) {
  return (
    perfil === 'COORD_RH' ||
    (perfil === 'RECRUTADOR' && vaga.recrutadorId === usuario) ||
    (perfil === 'GESTOR' && vaga.solicitanteId === usuario)
  )
}
export function proximosStatus(
  status: StatusVaga,
  perfil: Perfil,
  podeAlterar: boolean,
): StatusVaga[] {
  if (status === 'EM_APROVACAO')
    return ['COORD_RH', 'DIRETORIA'].includes(perfil) ? ['PUBLICADA', 'RASCUNHO'] : []
  if (!podeAlterar) return []
  return (
    (
      {
        RASCUNHO: ['EM_APROVACAO'],
        PUBLICADA: ['CONGELADA', 'FECHADA'],
        CONGELADA: ['PUBLICADA', 'FECHADA'],
        FECHADA: [],
      } as Record<string, StatusVaga[]>
    )[status] || []
  )
}
