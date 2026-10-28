import { describe, it, expect } from 'vitest'
import { criarEscalaGrafico, limitarPercentual } from '../../app/utils/graficos'

describe('Escala do histórico e apresentação de percentuais', () => {
  it.each([
    [[], 2],
    [[0, 0], 2],
    [[2, 5, 8], 8],
    [[3, 7], 8],
    [[-4, Number.NaN, Infinity], 2],
  ])('dimensiona os valores %j', (valores, limite) => {
    const escala = criarEscalaGrafico(valores as number[])
    expect(escala.limite).toBe(limite)
    expect(escala.marcacoes).toEqual([0, Number(limite) / 2, limite])
  })
  it.each([
    [80, 80],
    [-1, 0],
    [101, 100],
    [Number.NaN, 0],
    [Infinity, 0],
  ])('limita a barra visual de %s para %s', (entrada, resultado) => {
    expect(limitarPercentual(entrada)).toBe(resultado)
  })
})
