import { createCipheriv, createDecipheriv, createHash, randomBytes } from 'node:crypto'

export interface SessaoPrivada {
  accessToken: string
  refreshToken: string | null
  expiresAt: string
}

function chavePrivada(segredo: string) {
  if (segredo.length < 32)
    throw new Error('Configure NUXT_SESSION_SECRET com pelo menos 32 caracteres.')
  return createHash('sha256').update(segredo).digest()
}

export function cifrarSessao(sessao: SessaoPrivada, segredo: string): string {
  const vetor = randomBytes(12)
  const cifrador = createCipheriv('aes-256-gcm', chavePrivada(segredo), vetor)
  const conteudo = Buffer.concat([
    cifrador.update(JSON.stringify(sessao), 'utf8'),
    cifrador.final(),
  ])
  return [vetor, cifrador.getAuthTag(), conteudo]
    .map((parte) => parte.toString('base64url'))
    .join('.')
}

export function decifrarSessao(valor: string, segredo: string): SessaoPrivada | null {
  try {
    const partes = valor.split('.')
    if (partes.length !== 3 || partes.some((parte) => !/^[A-Za-z0-9_-]+$/.test(parte))) return null
    const [vetor, etiqueta, conteudo] = partes.map((parte) => Buffer.from(parte, 'base64url'))
    if (!vetor || !etiqueta || !conteudo || vetor.length !== 12 || etiqueta.length !== 16)
      return null
    const decifrador = createDecipheriv('aes-256-gcm', chavePrivada(segredo), vetor)
    decifrador.setAuthTag(etiqueta)
    const sessao = JSON.parse(
      Buffer.concat([decifrador.update(conteudo), decifrador.final()]).toString('utf8'),
    ) as Partial<SessaoPrivada>
    if (
      typeof sessao.accessToken !== 'string' ||
      !sessao.accessToken ||
      typeof sessao.expiresAt !== 'string' ||
      !Number.isFinite(Date.parse(sessao.expiresAt))
    )
      return null
    if (sessao.refreshToken !== null && typeof sessao.refreshToken !== 'string') return null
    return sessao as SessaoPrivada
  } catch {
    return null
  }
}
