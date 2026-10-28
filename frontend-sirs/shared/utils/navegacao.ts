export function destinoInterno(destino: unknown): string {
  if (
    typeof destino !== 'string' ||
    !destino.startsWith('/') ||
    destino.startsWith('//') ||
    /[\\\u0000-\u0020\u007f]/.test(destino) ||
    destino.includes('%')
  )
    return '/'
  return destino.startsWith('/login') ? '/' : destino
}
