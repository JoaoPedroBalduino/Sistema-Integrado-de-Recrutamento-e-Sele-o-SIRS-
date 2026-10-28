import type { Perfil } from '#shared/types/api'
export const perfisOperacionais: Perfil[] = [
  'ADMIN',
  'COORD_RH',
  'RECRUTADOR',
  'GESTOR',
  'DIRETORIA',
]
export const perfisCandidatos: Perfil[] = ['ADMIN', 'COORD_RH', 'RECRUTADOR', 'GESTOR']
export const perfisEdicaoCandidatos: Perfil[] = ['COORD_RH', 'RECRUTADOR']
export const perfisEdicaoVagas: Perfil[] = ['COORD_RH', 'RECRUTADOR', 'GESTOR']
export const perfisAdministracao: Perfil[] = ['ADMIN', 'COORD_RH']
export function possuiPerfil(perfil: Perfil | undefined, permitidos: readonly Perfil[]) {
  return perfil !== undefined && permitidos.includes(perfil)
}
export const navegacao: Array<{
  titulo: string
  caminho: string
  icone: string
  grupo: string
  perfis?: Perfil[]
}> = [
  { titulo: 'Visão geral', caminho: '/', icone: 'dashboard', grupo: 'PRINCIPAL' },
  {
    titulo: 'Vagas',
    caminho: '/vagas',
    icone: 'briefcase',
    grupo: 'PRINCIPAL',
    perfis: perfisOperacionais,
  },
  {
    titulo: 'Candidaturas',
    caminho: '/candidaturas',
    icone: 'kanban',
    grupo: 'PRINCIPAL',
    perfis: ['ADMIN', 'COORD_RH', 'RECRUTADOR', 'GESTOR', 'AVALIADOR'],
  },
  {
    titulo: 'Candidatos',
    caminho: '/candidatos',
    icone: 'users',
    grupo: 'PRINCIPAL',
    perfis: perfisCandidatos,
  },
  {
    titulo: 'Custos',
    caminho: '/custos',
    icone: 'wallet',
    grupo: 'GESTÃO',
    perfis: ['ADMIN', 'COORD_RH', 'RECRUTADOR', 'DIRETORIA'],
  },
  {
    titulo: 'Indicadores',
    caminho: '/indicadores',
    icone: 'chart',
    grupo: 'GESTÃO',
    perfis: perfisOperacionais,
  },
  {
    titulo: 'Cadastros',
    caminho: '/cadastros',
    icone: 'layers',
    grupo: 'GESTÃO',
    perfis: perfisOperacionais,
  },
  {
    titulo: 'Administração',
    caminho: '/administracao',
    icone: 'shield',
    grupo: 'SISTEMA',
    perfis: perfisAdministracao,
  },
  {
    titulo: 'Privacidade',
    caminho: '/privacidade',
    icone: 'lock',
    grupo: 'SISTEMA',
    perfis: ['ADMIN', 'COORD_RH'],
  },
  {
    titulo: 'Auditoria',
    caminho: '/auditoria',
    icone: 'history',
    grupo: 'SISTEMA',
    perfis: ['ADMIN', 'COORD_RH'],
  },
]
