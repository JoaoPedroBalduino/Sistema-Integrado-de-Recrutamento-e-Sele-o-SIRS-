import { describe, it, expect, vi } from 'vitest'
import {
  dataHoje,
  formatarData,
  formatarMoeda,
  formatarNota,
  iniciais,
} from '../../app/utils/formatacao'
import { possuiPerfil, navegacao } from '../../app/utils/permissoes'
import { proximosStatus } from '../../app/utils/vagas'
import { gerarCsv } from '../../app/utils/exportacao'
describe('Formatação e regras de apresentação', () => {
  it('mantém datas civis sem deslocar o dia', () =>
    expect(formatarData('2026-10-28')).toBe('28/10/2026'))
  it('trata datas inválidas', () => expect(formatarData('invalida')).toBe('—'))
  it('não inventa datas ausentes', () => expect(formatarData(null)).toBe('—'))
  it('formata a moeda brasileira', () =>
    expect(formatarMoeda(573.86).replace(/\s/g, ' ')).toBe('R$ 573,86'))
  it('respeita a nota normalizada entre zero e um', () => expect(formatarNota(0.8)).toBe('80%'))
  it('não troca ausência de nota por zero', () => expect(formatarNota(null)).toBe('Sem nota'))
  it('calcula iniciais com espaços extras', () => expect(iniciais('  Marina   Costa ')).toBe('MC'))
  it('nega perfil indefinido', () => expect(possuiPerfil(undefined, ['ADMIN'])).toBe(false))
  it('mantém diretoria fora de dados de candidatos', () =>
    expect(navegacao.find((item) => item.caminho === '/candidatos')!.perfis).not.toContain(
      'DIRETORIA',
    ))
  it('mantém gestor fora da consulta de custos', () =>
    expect(navegacao.find((item) => item.caminho === '/custos')!.perfis).not.toContain('GESTOR'))
  it('permite auditoria para coordenação', () =>
    expect(navegacao.find((item) => item.caminho === '/auditoria')!.perfis).toContain('COORD_RH'))
  it('não concede alteração operacional ao administrador', () =>
    expect(proximosStatus('RASCUNHO', 'ADMIN', false)).toEqual([]))
  it('mantém vaga fechada como estado final', () =>
    expect(proximosStatus('FECHADA', 'COORD_RH', true)).toEqual([]))
  it('permite aprovação da diretoria', () =>
    expect(proximosStatus('EM_APROVACAO', 'DIRETORIA', false)).toEqual(['PUBLICADA', 'RASCUNHO']))
  it('não permite publicação por recrutador', () =>
    expect(proximosStatus('EM_APROVACAO', 'RECRUTADOR', true)).toEqual([]))
})
describe('Datas civis brasileiras', () => {
  it('exibe instantes no fuso de Sao Paulo', () =>
    expect(formatarData('2026-10-28T01:00:00Z')).toBe('27/10/2026'))
  it('calcula hoje sem depender do fuso do servidor', () => {
    vi.useFakeTimers()
    try {
      vi.setSystemTime(new Date('2026-10-28T01:00:00Z'))
      expect(dataHoje()).toBe('2026-10-27')
    } finally {
      vi.useRealTimers()
    }
  })
})

describe('Exportação CSV', () => {
  it('escapa aspas', () => expect(gerarCsv(['Nome'], [['Pessoa "A"']])).toContain('"Pessoa ""A"""'))
  it('mantém delimitadores dentro da célula', () =>
    expect(gerarCsv(['Nome'], [['Pessoa; área']])).toContain('"Pessoa; área"'))
  it.each(['=HYPERLINK("x")', '+COMANDO', '-COMANDO', '@COMANDO', '  =FORMULA'])(
    'neutraliza fórmula %s',
    (valor) => expect(gerarCsv(['Nome'], [[valor]])).toContain("'" + valor.replaceAll('"', '""')),
  )
  it('preserva valores numéricos negativos como números', () =>
    expect(gerarCsv(['Valor'], [[-5]])).toContain('"-5"'))
})
