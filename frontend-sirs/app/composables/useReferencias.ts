import type { RespostaPaginada } from '#shared/types/api'
export function useReferencias() {
  const consulta = useApiSirs()
  return async <Registro>(
    caminho: string,
    filtros: Record<string, string | number | boolean | undefined> = {},
  ): Promise<Registro[]> => {
    const registros: Registro[] = []
    for (let pagina = 0; pagina < 100; pagina++) {
      const resposta = await consulta<RespostaPaginada<Registro>>(caminho, {
        query: { ...filtros, page: pagina, size: 100 },
      })
      registros.push(...resposta.content)
      if (pagina + 1 >= resposta.totalPages) return registros
    }
    throw new Error('A lista excede 10.000 registros. Refine os filtros para continuar.')
  }
}
