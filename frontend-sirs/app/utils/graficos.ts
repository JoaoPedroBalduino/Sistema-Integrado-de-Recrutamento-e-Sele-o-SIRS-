export function limitarPercentual(valor: number) {
  return Number.isFinite(valor) ? Math.min(100, Math.max(0, valor)) : 0
}

export function criarEscalaGrafico(valores: readonly number[]) {
  const maiorValor = Math.max(0, ...valores.filter(Number.isFinite))
  const limite = Math.max(2, Math.ceil(maiorValor / 2) * 2)
  return { limite, marcacoes: [0, limite / 2, limite] }
}
