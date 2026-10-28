import type { RespostaPaginada } from '#shared/types/api'
export function useColecao<Registro>(
  caminho: string,
  filtros: Ref<Record<string, string | number | boolean | undefined>> = ref({}),
) {
  const pagina = ref(0)
  const consulta = useApiSirs()
  watch(
    filtros,
    () => {
      pagina.value = 0
    },
    { deep: true },
  )
  const recurso = useAsyncData(
    caminho,
    () =>
      consulta<RespostaPaginada<Registro>>(caminho, {
        query: { page: pagina.value, size: 10, ...filtros.value },
      }),
    { watch: [pagina, filtros] },
  )
  return { ...recurso, pagina }
}
