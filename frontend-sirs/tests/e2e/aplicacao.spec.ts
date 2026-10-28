import { test, expect, type Page } from '@playwright/test'
const senha = 'SenhaTesteFrontend123!'
async function entrar(pagina: Page, email = 'coordenacao@exemplo.test') {
  await pagina.goto('/login')
  await pagina.getByLabel('E-mail corporativo').fill(email)
  await pagina.getByLabel('Senha', { exact: true }).fill(senha)
  await pagina.getByRole('button', { name: 'Entrar', exact: true }).click()
  await expect(pagina).toHaveURL(/\/$/)
  await expect(pagina.getByRole('button', { name: 'Sair da conta', exact: true })).toBeVisible()
}
async function fecharSucesso(pagina: Page) {
  const mensagem = pagina.locator('.message-dialog[data-tipo="sucesso"]')
  await expect(mensagem).toBeVisible()
  await mensagem.getByRole('button', { name: 'Entendi', exact: true }).click()
  await expect(mensagem).toHaveCount(0)
}
async function navegar(pagina: Page, titulo: string) {
  const abrir = pagina.getByRole('button', { name: 'Abrir menu' })
  if (await abrir.isVisible()) await abrir.click()
  await pagina
    .getByRole('navigation', { name: 'Navegação principal' })
    .getByRole('link', { name: titulo, exact: true })
    .click()
}
test.beforeEach(async ({ request }) => {
  await request.post('http://127.0.0.1:4100/__test__/reset')
})

test('redireciona rota protegida para o login', async ({ page }) => {
  await page.goto('/vagas')
  await expect(page).toHaveURL(/\/login\?destino=/)
  await expect(page.getByLabel('E-mail corporativo')).toBeVisible()
})

test('mostra erro de credenciais e permite tentar novamente', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('E-mail corporativo').fill('coordenacao@exemplo.test')
  await page.getByLabel('Senha', { exact: true }).fill('senha-incorreta')
  await page.getByRole('button', { name: 'Entrar', exact: true }).click()
  await expect(page.getByRole('alertdialog')).toContainText('Credenciais inválidas')
  await page.getByRole('alertdialog').getByRole('button', { name: 'Entendi', exact: true }).click()
  await expect(page.getByLabel('Senha', { exact: true })).toHaveValue('')
})

test('autentica sem expor tokens e renderiza o painel', async ({ page, context }) => {
  await entrar(page)
  await expect(page.getByRole('heading', { name: /Marina/ })).toBeVisible()
  await expect(page.getByText('Vagas publicadas', { exact: true })).toBeVisible()
  const cookies = await context.cookies()
  const sessao = cookies.find((cookie) => cookie.name === 'sirs-session')
  expect(sessao?.httpOnly).toBe(true)
  expect(sessao?.sameSite).toBe('Lax')
  expect(sessao?.value).not.toContain('access-fixture')
  const armazenamento = await page.evaluate(
    () => document.cookie + JSON.stringify(localStorage) + JSON.stringify(sessionStorage),
  )
  expect(armazenamento).not.toContain('sirs-session')
  expect(armazenamento).not.toContain('access-fixture')
  expect(armazenamento).not.toContain('refresh-fixture')
  const html = await page.content()
  expect(html).not.toContain('chave-ficticia-exclusiva-da-suite')
  expect(html).not.toContain('http://127.0.0.1:4100')
  expect(html).not.toContain('access-fixture-')
  await page.screenshot({ path: test.info().outputPath('painel.png'), fullPage: true })
})

test('filtra vagas no servidor e percorre páginas', async ({ page, request }) => {
  await entrar(page)
  await navegar(page, 'Vagas')
  await expect(page.getByRole('heading', { name: 'Vagas', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Próxima', exact: true }).click()
  await expect(page.getByText(/Página 2 de 2/)).toBeVisible()
  await page.getByRole('button', { name: 'Publicada', exact: true }).click()
  await expect(page.getByText(/Página 1 de 1/)).toBeVisible()
  const estado = await (await request.get('http://127.0.0.1:4100/__test__/state')).json()
  expect(
    estado.requests.some(
      (item: { path: string; query: { status?: string } }) =>
        item.path === '/api/v1/vagas' && item.query.status === 'PUBLICADA',
    ),
  ).toBe(true)
})

test('cria vaga com referências e acompanha a estrutura', async ({ page }) => {
  await entrar(page)
  await page.getByRole('link', { name: 'Nova vaga', exact: true }).click()
  await page.getByLabel('Título da vaga').fill('Engenheiro de plataforma')
  await page.getByLabel('Cargo', { exact: true }).selectOption('3')
  await page.getByLabel('Departamento', { exact: true }).selectOption('2')
  await page.getByLabel('Solicitante', { exact: true }).selectOption('3')
  await page.getByLabel('Recrutador responsável').selectOption('7')
  await page.getByLabel('Canais de divulgação').selectOption('1')
  await page.getByRole('button', { name: 'Salvar vaga', exact: true }).click()
  await fecharSucesso(page)
  await expect(
    page.getByRole('heading', { name: 'Engenheiro de plataforma', exact: true }),
  ).toBeVisible()
  await page.getByRole('tab', { name: 'Etapas', exact: true }).click()
  await page.getByRole('button', { name: 'Adicionar etapa', exact: true }).click()
  await page.getByLabel('Nome', { exact: true }).fill('Conversa com a equipe')
  await page.getByRole('button', { name: 'Salvar alterações', exact: true }).click()
  await fecharSucesso(page)
  await expect(page.getByText('Conversa com a equipe', { exact: true })).toBeVisible()
})

test('salva cadastro, trata erro de negócio e permite edição', async ({ page }) => {
  await entrar(page)
  await navegar(page, 'Cadastros')
  await page.getByRole('button', { name: 'Novo registro', exact: true }).click()
  await page.getByLabel('Nome', { exact: true }).fill('Duplicado')
  await page.getByRole('button', { name: 'Salvar alterações', exact: true }).click()
  await expect(page.getByRole('alertdialog')).toContainText('Nome já cadastrado')
  await page.getByRole('alertdialog').getByRole('button', { name: 'Entendi', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('Nome já cadastrado')
  await page.getByLabel('Nome', { exact: true }).fill('Operações e Pessoas')
  await page.getByRole('button', { name: 'Salvar alterações', exact: true }).click()
  await fecharSucesso(page)
  await expect(page.getByRole('cell', { name: 'Operações e Pessoas', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Editar Operações e Pessoas', exact: true }).click()
  await page.getByLabel('Cadastro ativo', { exact: true }).uncheck()
  await page.getByRole('button', { name: 'Salvar alterações', exact: true }).click()
  await fecharSucesso(page)
  await expect(page.getByRole('row').filter({ hasText: 'Operações e Pessoas' })).toContainText(
    'Desativado',
  )
})

test('cadastra candidato com base legal e pesquisa real na API', async ({ page }) => {
  await entrar(page)
  await navegar(page, 'Candidatos')
  await page.getByRole('button', { name: 'Novo candidato', exact: true }).click()
  await page.getByLabel('Nome completo').fill('Talento de Verificação')
  await page.getByLabel('E-mail', { exact: true }).fill('talento@exemplo.test')
  await page.getByLabel('Base legal').selectOption('LEGITIMO_INTERESSE')
  await page.getByLabel('Data e hora da coleta').fill('2026-10-01T10:00')
  await page.getByRole('button', { name: 'Salvar alterações', exact: true }).click()
  await fecharSucesso(page)
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(
    page.getByRole('link', { name: 'Talento de Verificação', exact: true }),
  ).toBeVisible()
  await page.getByLabel('Buscar candidato por nome ou e-mail').fill('Talento de Verificação')
  await page.getByRole('button', { name: 'Buscar', exact: true }).click()
  await expect(
    page.getByRole('link', { name: 'Talento de Verificação', exact: true }),
  ).toBeVisible()
  await expect(page.getByRole('link', { name: 'Fernanda Lima', exact: true })).toHaveCount(0)
})

test('abre candidatura e registra avaliação pelos critérios corretos', async ({
  page,
  request,
}) => {
  await entrar(page)
  await navegar(page, 'Candidaturas')
  await page.getByRole('heading', { name: 'Fernanda Lima', exact: true }).click()
  await expect(
    page.getByRole('heading', { name: 'Avaliação por critérios', exact: true }),
  ).toBeVisible()
  await page.getByLabel('Nota para Experiência técnica').fill('5')
  await page.getByLabel('Nota para Colaboração').fill('4')
  await page.getByRole('button', { name: 'Salvar avaliação', exact: true }).click()
  await expect(page.getByText('Avaliação registrada.', { exact: true })).toBeVisible()
  await fecharSucesso(page)
  const estado = await (await request.get('http://127.0.0.1:4100/__test__/state')).json()
  expect(
    estado.requests.find(
      (item: { path: string; method: string }) =>
        item.path === '/api/v1/candidaturas/1/avaliacoes' && item.method === 'PUT',
    )?.body.notas,
  ).toEqual([
    { criterioId: 1, nota: 5 },
    { criterioId: 2, nota: 4 },
  ])
})

test('avaliador acessa a ficha designada sem controles de contratação', async ({ page }) => {
  await entrar(page, 'avaliador@exemplo.test')
  await expect(page.getByRole('link', { name: 'Administração', exact: true })).toHaveCount(0)
  await navegar(page, 'Candidaturas')
  await page.getByRole('heading', { name: 'Fernanda Lima', exact: true }).click()
  await expect(page.getByLabel('Nota para Experiência técnica')).toBeEnabled()
  await expect(page.getByRole('button', { name: 'Registrar decisão', exact: true })).toHaveCount(0)
  await expect(
    page.getByRole('heading', { name: 'Proposta e contratação', exact: true }),
  ).toHaveCount(0)
})

test('nega rota de administração ao recrutador', async ({ page }) => {
  await entrar(page, 'recrutador@exemplo.test')
  await page.goto('/administracao')
  await expect(page.getByRole('heading', { name: 'Este espaço exige outro perfil.' })).toBeVisible()
})

test('administrador cria conta sem retornar senha ao navegador', async ({ page, request }) => {
  await entrar(page, 'admin@exemplo.test')
  await navegar(page, 'Administração')
  await page.getByRole('button', { name: 'Novo usuário', exact: true }).click()
  await page.getByLabel('Nome completo').fill('Pessoa da Equipe')
  await page.getByLabel('E-mail corporativo').fill('pessoa-equipe@exemplo.test')
  await page.getByLabel('Senha inicial').fill('SenhaDaNovaPessoa123!')
  await page.getByLabel('Perfil de acesso').selectOption('GESTOR')
  await page.getByRole('button', { name: 'Salvar alterações', exact: true }).click()
  await fecharSucesso(page)
  await expect(page.getByRole('cell').filter({ hasText: 'Pessoa da Equipe' })).toBeVisible()
  const usuarios = await page.request.get('/api/backend/usuarios')
  expect(usuarios.status()).toBe(200)
  expect(JSON.stringify(await usuarios.json())).not.toContain('SenhaDaNovaPessoa123!')
})

test('bloqueia CSRF e mantém a sessão legítima', async ({ page, request }) => {
  await entrar(page)
  const rejeitada = await page.request.post('/api/auth/logout', {
    data: {},
    headers: { Origin: 'https://outro-site.example' },
  })
  expect(rejeitada.status()).toBe(403)
  const sessao = await page.request.get('/api/auth/session')
  expect((await sessao.json()).usuario.perfil).toBe('COORD_RH')
})

test('não transforma o proxy em uma rota de autenticação arbitrária', async ({ page, request }) => {
  await entrar(page)
  expect((await request.get('/api/backend/auth/me')).status()).toBe(404)
  expect((await request.get('/api/backend/vagas/%252e%252e/usuarios')).status()).toBe(404)
})

test('renova sessão expirada no SSR e encaminha o novo cookie', async ({
  page,
  context,
  request,
}) => {
  await entrar(page, 'expirada@exemplo.test')
  const antes = (await context.cookies()).find((cookie) => cookie.name === 'sirs-session')?.value
  await page.goto('/vagas')
  await expect(page.getByRole('heading', { name: 'Vagas', exact: true })).toBeVisible()
  expect((await context.cookies()).find((cookie) => cookie.name === 'sirs-session')?.httpOnly).toBe(
    true,
  )
  const estado = await (await request.get('http://127.0.0.1:4100/__test__/state')).json()
  expect(estado.refreshCount).toBe(1)
  expect(antes).toBeTruthy()
})

test('cookie adulterado volta ao login', async ({ page, context }) => {
  await entrar(page)
  const cookie = (await context.cookies()).find((cookie) => cookie.name === 'sirs-session')!
  await context.addCookies([{ ...cookie, value: 'conteudo-adulterado' }])
  await page.goto('/vagas')
  await expect(page).toHaveURL(/\/login/)
})

test('exporta indicadores e mostra custos calculados pela API', async ({ page }) => {
  await entrar(page)
  await navegar(page, 'Indicadores')
  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Exportar histórico', exact: true }).click()
  expect((await download).suggestedFilename()).toBe('indicadores-sirs.csv')
  await navegar(page, 'Custos')
  await expect(
    page.getByRole('heading', { name: 'De onde vem o investimento', exact: true }),
  ).toBeVisible()
  await expect(page.getByText(/124,91/)).toBeVisible()
})

test('exibe indisponibilidade sem usar dados fictícios na aplicação', async ({ page, request }) => {
  await entrar(page)
  await request.post('http://127.0.0.1:4100/__test__/outage')
  await navegar(page, 'Cadastros')
  await expect(page.getByRole('alert')).toContainText('backend está indisponível')
  await request.post('http://127.0.0.1:4100/__test__/outage', { data: { enabled: false } })
  await page.getByRole('button', { name: 'Tentar novamente', exact: true }).click()
  await expect(page.getByRole('cell', { name: 'Produto e Design', exact: true })).toBeVisible()
})

test('logout limpa o cookie e bloqueia o retorno a páginas protegidas', async ({
  page,
  context,
}) => {
  await entrar(page)
  await page.getByRole('button', { name: 'Sair da conta', exact: true }).click()
  await expect(page).toHaveURL(/\/login$/)
  expect((await context.cookies()).some((cookie) => cookie.name === 'sirs-session')).toBe(false)
  await page.goto('/vagas')
  await expect(page).toHaveURL(/\/login/)
})

test('layout não transborda no tamanho de tela escolhido', async ({ page }) => {
  await entrar(page)
  await expect(page.getByRole('heading', { name: /Marina/ })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  )
  await page.screenshot({ path: test.info().outputPath('responsivo.png'), fullPage: true })
  await navegar(page, 'Vagas')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  )
})
