import { describe, it, expect } from 'vitest'
import { readFileSync, existsSync } from 'node:fs'
import { createHash } from 'node:crypto'

const caminho = (arquivo: string) => new URL('../../' + arquivo, import.meta.url)

describe('Identidade visual SIRS', () => {
  it.each([
    ['simbolo-sirs.png', 'e23745176e876abdc52ae72be65aba4aa4aee9d8d1c2713f8495a480c9557840'],
    [
      'logo-sirs-horizontal.png',
      '9a214391ce98834479902b5d4d4d29c17fc9c9e6680f3e383708b86e53f67c68',
    ],
  ])('preserva a imagem original %s', (arquivo, hash) => {
    const imagem = readFileSync(caminho('public/branding/' + arquivo))
    expect(createHash('sha256').update(imagem).digest('hex')).toBe(hash)
  })

  it('publica um favicon PNG quadrado e transparente', () => {
    const imagem = readFileSync(caminho('public/favicon.png'))
    expect(imagem.subarray(0, 8).toString('hex')).toBe('89504e470d0a1a0a')
    expect(imagem.readUInt32BE(16)).toBe(imagem.readUInt32BE(20))
    expect(imagem.readUInt32BE(16)).toBeGreaterThanOrEqual(32)
    expect(imagem[25]).toBe(6)
    expect(readFileSync(caminho('nuxt.config.ts'), 'utf8')).toContain(
      "type: 'image/png', href: '/favicon.png'",
    )
    expect(existsSync(caminho('public/favicon.svg'))).toBe(false)
  })

  it('reutiliza a marca com nome acessível e enquadramento proporcional', () => {
    const componente = readFileSync(caminho('app/components/SirsMarca.vue'), 'utf8')
    expect(componente).toContain('aria-label="SIRS · Recrutamento e seleção"')
    expect(componente).toContain('href="/branding/logo-sirs-horizontal.png"')
    expect(componente).toContain('href="/branding/simbolo-sirs.png"')
    for (const arquivo of ['app/pages/login.vue', 'app/layouts/default.vue', 'app/error.vue']) {
      const fonte = readFileSync(caminho(arquivo), 'utf8')
      expect(fonte).toContain('<SirsMarca')
      expect(fonte).not.toContain('brand-symbol')
    }
  })
})
