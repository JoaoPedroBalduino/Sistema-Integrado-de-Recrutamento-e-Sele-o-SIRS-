export function gerarCsv(
  cabecalhos: string[],
  linhas: Array<Array<string | number | null | undefined>>,
): string {
  function escapar(valor: string | number | null | undefined) {
    let texto = String(valor ?? '')
    if (typeof valor === 'string' && /^[\s]*[=+@-]/.test(texto)) texto = "'" + texto
    return '"' + texto.replaceAll('"', '""') + '"'
  }
  return [cabecalhos, ...linhas].map((linha) => linha.map(escapar).join(';')).join('\r\n')
}
export function baixarArquivo(nome: string, conteudo: string, tipo = 'text/csv;charset=utf-8') {
  const endereco = URL.createObjectURL(new Blob([conteudo], { type: tipo }))
  const ancora = document.createElement('a')
  ancora.href = endereco
  ancora.download = nome
  ancora.click()
  setTimeout(() => URL.revokeObjectURL(endereco), 1000)
}
