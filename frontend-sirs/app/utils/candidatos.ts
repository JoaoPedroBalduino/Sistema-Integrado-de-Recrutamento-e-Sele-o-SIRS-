import type { CampoFormulario } from '#shared/types/formulario'
export const camposCandidato: CampoFormulario[] = [
  { chave: 'nome', rotulo: 'Nome completo', obrigatorio: true, maximo: 150 },
  { chave: 'email', rotulo: 'E-mail', tipo: 'email', obrigatorio: true, maximo: 255 },
  { chave: 'curriculoUrl', rotulo: 'Link do currículo (HTTPS)', maximo: 500 },
  {
    chave: 'baseLegal',
    rotulo: 'Base legal',
    tipo: 'select',
    obrigatorio: true,
    opcoes: [
      { valor: 'CONSENTIMENTO', rotulo: 'Consentimento' },
      { valor: 'LEGITIMO_INTERESSE', rotulo: 'Legítimo interesse' },
      { valor: 'EXECUCAO_CONTRATO', rotulo: 'Execução de contrato' },
    ],
  },
  {
    chave: 'dataColeta',
    rotulo: 'Data e hora da coleta',
    tipo: 'datetime-local',
    obrigatorio: true,
  },
  {
    chave: 'versaoConsentimento',
    rotulo: 'Versão do consentimento',
    maximo: 20,
    dica: 'Obrigatória quando a base legal é consentimento.',
  },
]
export function prepararCandidato(valores: Record<string, unknown>) {
  return { ...valores, dataColeta: new Date(String(valores.dataColeta)).toISOString() }
}
