import { createServer } from 'node:http'
import { randomUUID } from 'node:crypto'

// Fixtures sintéticas: este servidor pertence somente aos testes e não faz parte da aplicação.
const senhaFixture = 'SenhaTesteFrontend123!'
let estado
const nomesCandidatos = ['Fernanda Lima', 'Lucas Martins', 'Camila Rocha']
function apoio(identificador, nome) {
  return { identificador, nome, ativo: true }
}
function usuario(identificador, nome, email, perfil) {
  return { identificador, nome, email, perfil, ativo: true, bloqueado: false }
}
function reiniciar() {
  const usuarios = [
    usuario(1, 'Renzo Ribeiro', 'admin@exemplo.test', 'ADMIN'),
    usuario(2, 'Marina Costa', 'coordenacao@exemplo.test', 'COORD_RH'),
    usuario(3, 'Eduardo Santos', 'gestor@exemplo.test', 'GESTOR'),
    usuario(7, 'Ana Souza', 'recrutador@exemplo.test', 'RECRUTADOR'),
    usuario(8, 'Pedro Almeida', 'avaliador@exemplo.test', 'AVALIADOR'),
    usuario(9, 'Clara Matos', 'diretoria@exemplo.test', 'DIRETORIA'),
  ]
  const areas = [apoio(1, 'Produto e Design'), apoio(2, 'Tecnologia'), apoio(3, 'Comercial')]
  const cargos = [
    apoio(1, 'Product Designer'),
    apoio(2, 'Analista de Dados'),
    apoio(3, 'Desenvolvedor Full Stack'),
  ]
  const canais = [apoio(1, 'LinkedIn'), apoio(2, 'Indicações')]
  const titulos = [
    'Product Designer Pleno',
    'Analista de Dados Sênior',
    'Desenvolvedor Full Stack',
    'Analista de Marketing',
    'Especialista em Sucesso do Cliente',
    'Engenheiro de Software',
    'Assistente de Pessoas',
    'Analista Financeiro',
    'Designer de Produto',
    'Analista de Projetos',
    'Especialista de Qualidade',
    'Pessoa Desenvolvedora Frontend',
  ]
  const vagas = titulos.map((titulo, indice) => ({
    identificador: indice + 1,
    version: 0,
    titulo,
    cargo: cargos[indice % 3],
    area: areas[indice % 3],
    solicitanteId: 3,
    recrutadorId: 7,
    posicoes: (indice % 2) + 1,
    cidade: 'São Paulo',
    estado: 'SP',
    remoto: indice % 2 === 0,
    observacoes: 'Uma oportunidade para construir boas experiências em equipe.',
    status: ['PUBLICADA', 'EM_APROVACAO', 'RASCUNHO', 'PUBLICADA', 'CONGELADA'][indice % 5],
    dataAbertura: '2026-10-01',
    dataPublicacao: '2026-10-05',
    dataFechamento: null,
    prazoAlvoDias: 30,
    investimentoDivulgacao: 200,
    canais,
    candidatos: [24, 18, 8, 12, 5][indice % 5],
    posicoesPreenchidas: 0,
    diasAberto: 15,
  }))
  const candidatos = nomesCandidatos.map((nome, indice) => ({
    identificador: indice + 1,
    nome,
    email: 'pessoa' + (indice + 1) + '@exemplo.test',
    curriculoUrl: null,
    baseLegal: 'CONSENTIMENTO',
    dataColeta: '2026-10-01T12:00:00Z',
    versaoConsentimento: '1.0',
    anonimizadoEm: null,
  }))
  const candidaturas = candidatos.map((candidato, indice) => ({
    identificador: indice + 1,
    vagaId: 1,
    tituloVaga: vagas[0].titulo,
    candidatoId: candidato.identificador,
    nomeCandidato: candidato.nome,
    emailCandidato: candidato.email,
    canalId: 1,
    etapaAtualId: indice ? 2 : 1,
    etapaAtual: indice ? 'Entrevista' : 'Triagem',
    status: 'EM_AVALIACAO',
    notaFinal: [0.86, 0.72, 0.68][indice],
    contratadaEm: null,
  }))
  estado = {
    usuarios,
    areas,
    cargos,
    canais,
    vagas,
    candidatos,
    candidaturas,
    etapas: [
      { identificador: 1, nome: 'Triagem', descricao: 'Primeiro encontro com o talento', ordem: 1 },
      {
        identificador: 2,
        nome: 'Entrevista',
        descricao: 'Uma conversa sobre a oportunidade',
        ordem: 2,
      },
    ],
    criterios: [
      {
        identificador: 1,
        nome: 'Experiência técnica',
        descricao: 'Conhecimento para os desafios da posição',
        peso: 3,
      },
      { identificador: 2, nome: 'Colaboração', descricao: 'Construção em equipe', peso: 2 },
    ],
    parametros: [
      {
        identificador: 1,
        chave: 'SALARIO_RECRUTADOR',
        valor: 1400,
        vigenciaInicio: '2026-01-01',
        vigenciaFim: null,
      },
    ],
    metas: [
      {
        identificador: 1,
        chave: 'VAGAS_NO_PRAZO',
        valor: 80,
        vigenciaInicio: '2026-01-01',
        vigenciaFim: null,
      },
    ],
    visitas: [],
    solicitacoes: [],
    auditoria: [],
    tokens: new Map(),
    renovacoes: new Map(),
    senhas: new Map(usuarios.map((pessoa) => [pessoa.identificador, senhaFixture])),
    requests: [],
    refreshCount: 0,
    indisponivel: false,
  }
}
reiniciar()
function paginar(registros, url) {
  const page = Number(url.searchParams.get('page') || 0)
  const size = Math.min(100, Number(url.searchParams.get('size') || 10))
  return {
    content: registros.slice(page * size, (page + 1) * size),
    paginacao: page,
    size,
    totalElements: registros.length,
    totalPages: Math.ceil(registros.length / size),
  }
}
function sessao(pessoa, expirada = false) {
  const token = 'access-fixture-' + randomUUID()
  const refresh = 'refresh-fixture-' + randomUUID()
  estado.tokens.set(token, pessoa.identificador)
  estado.renovacoes.set(refresh, pessoa.identificador)
  return {
    accessToken: token,
    refreshToken: refresh,
    expiresAt: new Date(Date.now() + (expirada ? -5000 : 3600000)).toISOString(),
    tokenType: 'Bearer',
    user: pessoa,
  }
}
const server = createServer(async (request, response) => {
  const url = new URL(request.url, 'http://127.0.0.1:4100')
  const path = (url.pathname.startsWith('/api/v1/') ? url.pathname.slice(8) : url.pathname)
    .split('/')
    .filter(Boolean)
  let dados = ''
  for await (const parte of request) dados += parte
  let corpo
  try {
    corpo = dados ? JSON.parse(dados) : {}
  } catch {
    response.writeHead(400)
    response.end()
    return
  }
  const enviar = (status, payload) => {
    response.writeHead(status, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' })
    response.end(status === 204 ? undefined : JSON.stringify(payload))
  }
  const falhar = (status, detail) =>
    enviar(status, { status, detail, requestId: 'fixture-request' })
  if (url.pathname === '/__test__/reset') {
    reiniciar()
    return enviar(200, { sucesso: true })
  }
  if (url.pathname === '/__test__/state')
    return enviar(200, { requests: estado.requests, refreshCount: estado.refreshCount })
  if (url.pathname === '/__test__/outage') {
    estado.indisponivel = corpo.enabled !== false
    return enviar(200, {})
  }
  if (url.pathname === '/health') return enviar(200, { status: 'UP' })
  estado.requests.push({
    method: request.method,
    path: url.pathname,
    query: Object.fromEntries(url.searchParams),
    ...(request.method !== 'GET' && path[0] !== 'auth' ? { body: corpo } : {}),
  })
  if (estado.indisponivel)
    return falhar(503, 'O backend está indisponível. Tente novamente em instantes.')
  if (path.join('/') === 'auth/login') {
    const pessoa =
      corpo.email === 'expirada@exemplo.test'
        ? estado.usuarios[1]
        : estado.usuarios.find((pessoa) => pessoa.email === corpo.email)
    if (
      !pessoa ||
      corpo.senha !== estado.senhas.get(pessoa.identificador) ||
      !pessoa.ativo ||
      pessoa.bloqueado
    )
      return falhar(401, 'Credenciais inválidas ou sessão indisponível.')
    return enviar(200, sessao(pessoa, corpo.email === 'expirada@exemplo.test'))
  }
  if (path.join('/') === 'auth/refresh') {
    const pessoa = estado.usuarios.find(
      (pessoa) => pessoa.identificador === estado.renovacoes.get(corpo.refreshToken),
    )
    if (!pessoa) return falhar(401, 'Sessão expirada.')
    estado.refreshCount++
    return enviar(200, sessao(pessoa))
  }
  const identificador = estado.tokens.get(
    (request.headers.authorization || '').replace('Bearer ', ''),
  )
  const pessoa = estado.usuarios.find((pessoa) => pessoa.identificador === identificador)
  if (!pessoa) return falhar(401, 'Autenticação necessária.')
  if (path.join('/') === 'auth/me') return enviar(200, pessoa)
  if (path.join('/') === 'auth/logout') {
    for (const [chave, valor] of estado.tokens)
      if (valor === identificador) estado.tokens.delete(chave)
    for (const [chave, valor] of estado.renovacoes)
      if (valor === identificador) estado.renovacoes.delete(chave)
    return enviar(204)
  }
  if (path.join('/') === 'auth/senha') {
    if (corpo.senhaAtual !== estado.senhas.get(identificador))
      return falhar(401, 'Senha atual inválida.')
    estado.senhas.set(identificador, corpo.novaSenha)
    for (const [chave, valor] of estado.tokens)
      if (valor === identificador) estado.tokens.delete(chave)
    return enviar(204)
  }
  if (path[0] === 'indicadores') {
    const resumo = {
      de: url.searchParams.get('de'),
      ate: url.searchParams.get('ate'),
      vagasFechadas: 8,
      vagasConsideradas: 10,
      vagasAtrasadas: 2,
      horasPorVagaFechada: 12.5,
      percentualNoPrazo: 80,
      custoPorVagaFechada: 573.86,
      mediaDiasAdmissao: 21,
      metasVigentes: { VAGAS_NO_PRAZO: 80 },
    }
    if (path[1] === 'resumo') return enviar(200, resumo)
    if (path[1] === 'historico')
      return enviar(
        200,
        [1, 2, 3, 4, 5, 6].map((mes, indice) => ({
          ...resumo,
          de: '2026-' + String(mes).padStart(2, '0') + '-01',
          vagasFechadas: [2, 5, 3, 7, 4, 8][indice],
        })),
      )
    if (path[1] === 'cargos')
      return enviar(
        200,
        estado.cargos.map((cargo) => ({
          cargoId: cargo.identificador,
          nome: cargo.nome,
          posicoesAbertas: 3,
          candidatos: 18,
          mediaDiasAberto: 21,
          investimentoMedio: 573.86,
        })),
      )
    if (path[1] === 'recrutadores')
      return enviar(200, [
        {
          usuarioId: 7,
          nome: 'Ana Souza',
          posicoesTrabalhadas: 10,
          posicoesPreenchidas: 8,
          percentualFechamento: 80,
        },
      ])
    if (path[1] === 'canais')
      return enviar(
        200,
        estado.canais.map((canal) => ({
          canalId: canal.identificador,
          nome: canal.nome,
          visitas: 120,
          contratacoes: 4,
          percentualCanal: 3.33,
          percentualGeral: 3.33,
        })),
      )
  }
  if (path[0] === 'vagas' && path[2] === 'custos') {
    if (request.method === 'PUT') return enviar(204)
    return enviar(200, {
      etapas: [
        { descricao: 'Briefing', horas: 1, custo: 10.82 },
        { descricao: 'Entrevistas', horas: 5, custo: 54.09 },
      ],
      custoHoras: 64.91,
      investimentosDiretos: 60,
      total: 124.91,
    })
  }
  if (path[0] === 'vagas' && path.length >= 2) {
    const vaga = estado.vagas.find((vaga) => vaga.identificador === Number(path[1]))
    if (!vaga) return falhar(404, 'Vaga não encontrada.')
    if (path.length === 2 && request.method === 'GET')
      return enviar(200, {
        vaga,
        etapas: estado.etapas,
        criterios: estado.criterios,
        historico: [
          {
            identificador: 1,
            statusAnterior: 'RASCUNHO',
            statusNovo: vaga.status,
            usuarioId: 2,
            alteradoEm: '2026-10-01T12:00:00Z',
          },
        ],
        apontamentos: [],
      })
    if (path[2] === 'status') {
      vaga.status = corpo.status
      return enviar(200, vaga)
    }
    if (path[2] === 'horas')
      return enviar(201, { identificador: 1, usuarioId: identificador, ...corpo })
    if (['etapas', 'criterios'].includes(path[2])) {
      const registros = estado[path[2]]
      if (request.method === 'DELETE') {
        const indice = registros.findIndex((registro) => registro.identificador === Number(path[3]))
        if (indice >= 0) registros.splice(indice, 1)
        return enviar(204)
      }
      const registro = path[3]
        ? registros.find((registro) => registro.identificador === Number(path[3]))
        : { identificador: registros.length + 1 }
      Object.assign(registro, corpo)
      if (!path[3]) registros.push(registro)
      return enviar(path[3] ? 200 : 201, registro)
    }
    if (request.method === 'PUT') {
      Object.assign(vaga, corpo, {
        area: estado.areas.find((item) => item.identificador === corpo.areaId),
        cargo: estado.cargos.find((item) => item.identificador === corpo.cargoId),
      })
      return enviar(200, vaga)
    }
  }
  if (path[0] === 'candidaturas' && path.length >= 2) {
    const candidatura = estado.candidaturas.find(
      (registro) => registro.identificador === Number(path[1]),
    )
    if (!candidatura) return falhar(404, 'Candidatura não encontrada.')
    if (path.length === 2)
      return enviar(200, {
        candidatura,
        candidato: estado.candidatos.find(
          (candidato) => candidato.identificador === candidatura.candidatoId,
        ),
        notas: [{ criterioId: 1, nota: 4, avaliadorId: 8 }],
        historico: [{ etapaId: 1, usuarioId: 2, movidoEm: '2026-10-01T12:00:00Z' }],
        avaliadores: [8],
        proposta: null,
        etapas: estado.etapas,
        criterios: estado.criterios,
      })
    if (path[2] === 'avaliacoes') {
      candidatura.notaFinal = 0.8
      return enviar(200, candidatura)
    }
    if (path[2] === 'etapa') {
      candidatura.etapaAtualId = corpo.etapaId
      candidatura.etapaAtual = estado.etapas.find(
        (etapa) => etapa.identificador === corpo.etapaId,
      )?.nome
      return enviar(200, candidatura)
    }
    if (path[2] === 'status') {
      candidatura.status = corpo.status
      return enviar(200, candidatura)
    }
    if (path[2] === 'proposta') return enviar(201, { identificador: 1, ...corpo, dataAceite: null })
    if (path[2] === 'contratacao') {
      candidatura.contratadaEm = new Date().toISOString()
      return enviar(200, candidatura)
    }
    if (path[2] === 'avaliadores') return enviar(204)
  }
  if (path[0] === 'cadastros' && ['recrutadores', 'solicitantes'].includes(path[1]))
    return enviar(
      200,
      paginar(
        estado.usuarios
          .filter((usuario) =>
            (path[1] === 'recrutadores'
              ? ['RECRUTADOR', 'COORD_RH']
              : ['GESTOR', 'COORD_RH']
            ).includes(usuario.perfil),
          )
          .map((usuario) => ({ identificador: usuario.identificador, nome: usuario.nome })),
        url,
      ),
    )
  if (path[0] === 'usuarios' && !['ADMIN', 'COORD_RH'].includes(pessoa.perfil))
    return falhar(403, 'Acesso negado.')
  if (path[0] === 'privacidade' && path[1] === 'candidatos')
    return enviar(200, { candidato: estado.candidatos[0], candidaturas: [] })
  let registros =
    path[0] === 'cadastros'
      ? estado[path[1]]
      : path[0] === 'configuracoes'
        ? path[1] === 'metas'
          ? estado.metas
          : estado.parametros
        : path[0] === 'privacidade'
          ? estado.solicitacoes
          : path[0] === 'visitas-canais'
            ? estado.visitas
            : estado[path[0]]
  if (!Array.isArray(registros)) return falhar(404, 'Recurso não encontrado.')
  if (request.method === 'GET') {
    if (path[1] && ['candidatos', 'usuarios'].includes(path[0])) {
      const item = registros.find((registro) => registro.identificador === Number(path[1]))
      return item ? enviar(200, item) : falhar(404, 'Registro não encontrado.')
    }
    if (path[0] === 'vagas' && url.searchParams.get('status'))
      registros = registros.filter((registro) => registro.status === url.searchParams.get('status'))
    if (path[0] === 'candidatos' && url.searchParams.get('pesquisa'))
      registros = registros.filter((registro) =>
        (registro.nome + registro.email)
          .toLowerCase()
          .includes(url.searchParams.get('pesquisa').toLowerCase()),
      )
    if (path[0] === 'candidaturas') {
      if (url.searchParams.get('status'))
        registros = registros.filter(
          (registro) => registro.status === url.searchParams.get('status'),
        )
      if (url.searchParams.get('vagaId'))
        registros = registros.filter(
          (registro) => String(registro.vagaId) === url.searchParams.get('vagaId'),
        )
    }
    return enviar(200, paginar(registros, url))
  }
  const indice =
    path[0] === 'cadastros' || path[0] === 'configuracoes' || path[0] === 'privacidade'
      ? path[2]
      : path[1]
  if (request.method === 'POST' || (request.method === 'PUT' && !indice)) {
    if (corpo.nome === 'Duplicado') return falhar(422, 'Nome já cadastrado.')
    const identificador = Math.max(0, ...registros.map((registro) => registro.identificador)) + 1
    let item = { identificador, ...corpo }
    if (path[0] === 'vagas')
      item = {
        ...estado.vagas[0],
        ...item,
        status: 'RASCUNHO',
        area: estado.areas.find((area) => area.identificador === corpo.areaId),
        cargo: estado.cargos.find((cargo) => cargo.identificador === corpo.cargoId),
        canais: estado.canais.filter((canal) => corpo.canalIds.includes(canal.identificador)),
        candidatos: 0,
        posicoesPreenchidas: 0,
      }
    if (path[0] === 'usuarios') {
      estado.senhas.set(identificador, corpo.senha)
      item = usuario(identificador, corpo.nome, corpo.email, corpo.perfil)
    }
    if (path[0] === 'privacidade')
      item = {
        ...item,
        status: 'ABERTA',
        solicitadaEm: new Date().toISOString(),
        prazo: '2026-11-15',
        concluidaEm: null,
      }
    registros.push(item)
    return enviar(request.method === 'POST' ? 201 : 200, item)
  }
  const item = registros.find((registro) => registro.identificador === Number(indice))
  if (!item) return falhar(404, 'Registro não encontrado.')
  Object.assign(item, corpo)
  return enviar(path.at(-1) === 'vigencia' ? 204 : 200, item)
})
server.listen(Number(process.env.FIXTURE_PORT || 4100), '127.0.0.1', () =>
  console.log('Servidor de fixtures sintéticas ativo.'),
)
