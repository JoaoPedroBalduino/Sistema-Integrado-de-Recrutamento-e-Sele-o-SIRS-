const recursosPermitidos = new Set([
  'vagas',
  'candidatos',
  'candidaturas',
  'cadastros',
  'configuracoes',
  'usuarios',
  'visitas-canais',
  'indicadores',
  'privacidade',
  'auditoria',
])

export function caminhoPermitido(caminho: string): boolean {
  return (
    /^[a-z0-9-]+(?:\/[a-z0-9-]+)*$/.test(caminho) &&
    recursosPermitidos.has(caminho.split('/')[0] ?? '')
  )
}

export function origemPermitida(origem: string | undefined, esperada: string): boolean {
  try {
    if (!origem) return false
    const endereco = new URL(origem)
    return (
      endereco.origin === new URL(esperada).origin &&
      endereco.pathname === '/' &&
      !endereco.username &&
      !endereco.password &&
      !endereco.search &&
      !endereco.hash
    )
  } catch {
    return false
  }
}
