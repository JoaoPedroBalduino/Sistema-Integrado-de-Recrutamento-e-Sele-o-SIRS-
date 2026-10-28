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
