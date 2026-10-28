import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { join, extname } from 'node:path'
import { fileURLToPath } from 'node:url'

function arquivos(diretorio: string): string[] {
  return readdirSync(diretorio, { withFileTypes: true }).flatMap((entrada) => {
    const caminho = join(diretorio, entrada.name)
    return entrada.isDirectory() ? arquivos(caminho) : [caminho]
  })
}

describe('Interface simples e sem emojis', () => {
  it('mantém emojis fora dos fontes da interface', () => {
    const fontes = ['app', 'shared', 'public'].flatMap((pasta) =>
      arquivos(fileURLToPath(new URL('../../' + pasta, import.meta.url))),
    )
    for (const caminho of fontes.filter((caminho) =>
      ['.vue', '.ts', '.css', '.svg', '.json'].includes(extname(caminho)),
    )) {
      const conteudo = readFileSync(caminho, 'utf8')
      expect(conteudo, caminho).not.toMatch(/\p{Extended_Pictographic}/u)
    }
  })

  it('centraliza os ícones na biblioteca Lucide', () => {
    const icones = readFileSync(
      fileURLToPath(new URL('../../app/components/SirsIcone.vue', import.meta.url)),
      'utf8',
    )
    expect(icones).toContain("from '@lucide/vue'")
  })

  it('não injeta conteúdo HTML recebido do backend', () => {
    const paginas = arquivos(fileURLToPath(new URL('../../app/pages', import.meta.url)))
    for (const caminho of paginas) expect(readFileSync(caminho, 'utf8')).not.toContain('v-html')
  })
})
