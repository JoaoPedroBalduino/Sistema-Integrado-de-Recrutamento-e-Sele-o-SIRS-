import { test, expect } from '@playwright/test'

test.beforeEach(async ({ request }) => {
  await request.post('http://127.0.0.1:4100/__test__/reset')
})

test('apresenta a logo original no login e carrega o favicon', async ({ page, request }) => {
  await page.goto('/login')
  const marca = page.getByRole('img', { name: 'SIRS · Recrutamento e seleção', exact: true })
  await expect(marca).toBeVisible()
  await expect(marca.locator('image')).toHaveAttribute('href', '/branding/logo-sirs-horizontal.png')
  await expect(page.locator('link[rel="icon"]')).toHaveAttribute('href', '/favicon.png')
  for (const arquivo of [
    '/branding/logo-sirs-horizontal.png',
    '/branding/simbolo-sirs.png',
    '/favicon.png',
  ]) {
    const resposta = await request.get(arquivo)
    expect(resposta.status()).toBe(200)
    expect(resposta.headers()['content-type']).toContain('image/png')
    expect((await resposta.body()).subarray(0, 8).toString('hex')).toBe('89504e470d0a1a0a')
  }
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({ path: test.info().outputPath('login-marca.png'), fullPage: true })
})

test('mantém a marca visível e proporcional no menu', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('E-mail corporativo').fill('coordenacao@exemplo.test')
  await page.getByLabel('Senha', { exact: true }).fill('SenhaTesteFrontend123!')
  await page.getByRole('button', { name: 'Entrar', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Sair da conta', exact: true })).toBeVisible()
  const abrir = page.getByRole('button', { name: 'Abrir menu' })
  if (await abrir.isVisible()) await abrir.click()
  const marca = page.getByRole('img', { name: 'SIRS · Recrutamento e seleção', exact: true })
  await expect(marca).toBeVisible()
  await expect.poll(async () => (await marca.boundingBox())!.x).toBeGreaterThanOrEqual(0)
  const tamanho = await marca.boundingBox()
  expect(tamanho!.width / tamanho!.height).toBeCloseTo(810 / 253, 1)
  await expect(
    page.getByRole('link', { name: 'SIRS · Página inicial', exact: true }),
  ).toHaveAttribute('href', '/')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({ path: test.info().outputPath('menu-marca.png'), fullPage: true })
})

test('organiza vagas e candidaturas por prioridade em cada tamanho de tela', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByRole('heading', { name: 'Entre na sua conta', exact: true })).toBeVisible()
  await page.getByLabel('E-mail corporativo').fill('coordenacao@exemplo.test')
  await page.getByLabel('Senha', { exact: true }).fill('SenhaTesteFrontend123!')
  await page.getByRole('button', { name: 'Entrar', exact: true }).click()
  await expect(page.getByRole('heading', { name: /Marina/ })).toBeVisible()
  const principal = page.locator('.dashboard-main')
  const candidaturas = page.locator('.recent-applications')
  await expect(
    principal.getByRole('heading', { name: 'Vagas recentes', exact: true }),
  ).toBeVisible()
  await expect(
    candidaturas.getByRole('heading', { name: 'Últimas candidaturas', exact: true }),
  ).toBeVisible()
  const vagas = (await principal.boundingBox())!
  const recentes = (await candidaturas.boundingBox())!
  if (page.viewportSize()!.width > 1200) {
    expect(recentes.x).toBeGreaterThan(vagas.x + vagas.width)
    expect(Math.abs(recentes.y - vagas.y)).toBeLessThan(2)
  } else {
    expect(recentes.y).toBeGreaterThanOrEqual(vagas.y + vagas.height)
  }
  await expect(
    page.getByRole('link', { name: 'Todas as candidaturas', exact: true }),
  ).toHaveAttribute('href', '/candidaturas')
  await page.screenshot({ path: test.info().outputPath('painel-organizado.png'), fullPage: true })
  const abrir = page.getByRole('button', { name: 'Abrir menu' })
  if (await abrir.isVisible()) await abrir.click()
  await page
    .getByRole('navigation', { name: 'Navegação principal' })
    .getByRole('link', { name: 'Vagas', exact: true })
    .click()
  await expect(page.getByRole('heading', { name: 'Vagas', exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({ path: test.info().outputPath('vagas-organizadas.png'), fullPage: true })
  for (const largura of [320, 768, 1024]) {
    await page.setViewportSize({ width: largura, height: 900 })
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  }
})

test('permite conferir a senha e avisa sobre Caps Lock no login', async ({ page }) => {
  await page.goto('/login')
  const senha = page.getByLabel('Senha', { exact: true })
  await senha.fill('SenhaTesteFrontend123!')
  await expect(senha).toHaveAttribute('type', 'password')
  await page.getByRole('button', { name: 'Mostrar senha', exact: true }).click()
  await expect(senha).toHaveAttribute('type', 'text')
  await expect(senha).toHaveValue('SenhaTesteFrontend123!')
  await expect(page.getByRole('button', { name: 'Ocultar senha', exact: true })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await page.getByRole('button', { name: 'Ocultar senha', exact: true }).click()
  await expect(senha).toHaveAttribute('type', 'password')
  await senha.dispatchEvent('keyup', { key: 'A', modifierCapsLock: true })
  await expect(page.getByRole('status')).toHaveText('Caps Lock ativado')
  await expect(senha).toHaveAttribute('aria-describedby', 'aviso-maiusculas')
  await senha.dispatchEvent('keyup', { key: 'a', modifierCapsLock: false })
  await expect(page.getByRole('status')).toHaveCount(0)
  for (const largura of [320, 768, 1024]) {
    await page.setViewportSize({ width: largura, height: 900 })
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await expect(page.getByRole('button', { name: 'Entrar', exact: true })).toBeVisible()
  }
})
