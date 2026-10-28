# SIRS — frontend Vue e Nuxt

Aplicação em **Vue 3, Nuxt 4 e TypeScript**, baseada nas nove telas de `prototipo-sirs` e integrada à API Java. As credenciais Supabase permanecem exclusivamente no backend.

## Executar

Requisitos: Node **22.22.3+, 24.15+ ou 26+** e backend SIRS em execução. Validado com Node 24.

```powershell
cd frontend-sirs
npm ci
# Apenas se ainda não existir uma .env:
Copy-Item .env.example .env
# Gere a chave e coloque o resultado em NUXT_SESSION_SECRET na .env:
node -e "console.log(require('node:crypto').randomBytes(48).toString('base64url'))"
npm run dev
```

Acesse **http://127.0.0.1:3000**. Não sobrescreva uma `.env` já configurada. Mantenha a chave estável entre reinicializações e fora do Git. O `.env.example` nunca deve receber valores reais.

| Variável privada          | Finalidade                                                             |
| ------------------------- | ---------------------------------------------------------------------- |
| `NUXT_BACKEND_BASE_URL`   | API vista pelo Nuxt; padrão `http://127.0.0.1:8080/api/v1`             |
| `NUXT_APP_ORIGIN`         | Origem exata aberta no navegador; padrão `http://127.0.0.1:3000`       |
| `NUXT_SESSION_SECRET`     | Chave aleatória de pelo menos 32 caracteres para criptografar a sessão |
| `DOCKER_BACKEND_BASE_URL` | Opcional: endereço do backend visto pelo container                     |

`localhost` e `127.0.0.1` são origens distintas. Ao trocar host ou porta, atualize `NUXT_APP_ORIGIN`. Em acesso público, configure HTTPS e a origem pública exata.

Para Supabase, inicie o backend com o perfil `supabase` conforme o README de `backend-sirs`. O administrador inicial deve estar configurado na `.env` do backend. Não há contas ou senhas padrão no frontend.

## Organização

```text
app/
  pages/           rotas e composição das telas
  layouts/         shell autenticado e acesso público
  components/      tabela, formulário, modal e estados reutilizáveis
  composables/     autenticação, API, paginação e formulários
  utils/           formatação, permissões e apresentação do domínio
  assets/css/      identidade visual e responsividade
server/
  api/auth/        login, sessão, logout e troca de senha
  api/backend/     proxy restrito para a API Java
  middleware/      proteção de origem e cabeçalhos de segurança
  utils/           criptografia e comunicação autenticada
shared/
  types/           contratos espelhados da API Java
  constants/       opções dos domínios do backend
```

## Sessão e segurança

- O navegador conversa apenas com o Nuxt na mesma origem. O Nuxt encaminha chamadas autenticadas à API Java.
- Access e refresh tokens ficam em cookie **HttpOnly, SameSite=Lax**, criptografado e autenticado com **AES-256-GCM**. Em HTTPS, recebe `Secure` e o prefixo `__Host-`.
- Tokens e chaves não aparecem no JSON de login, no payload de hidratação ou em `localStorage`/`sessionStorage`.
- Mutações exigem `Origin` correspondente à configuração. O proxy permite apenas recursos conhecidos e rejeita URLs arbitrárias e travessia de caminhos.
- Sessões Supabase são renovadas no servidor, sincronizando requisições concorrentes e encaminhando o cookie atualizado inclusive no SSR. O modo local solicita novo login ao expirar.
- As permissões visuais acompanham os seis perfis. O backend continua sendo a autoridade para perfil, responsabilidade pela vaga e designação do avaliador.
- Logout e troca de senha limpam a sessão local. Falhas reais exibem mensagens, erros por campo e opção de tentar novamente, sem substituir dados por fixtures.
- Fontes e ícones são servidos pela própria aplicação, sem fontes remotas ou analytics.

A sincronização de refresh usa memória do processo por até dez segundos. Em múltiplas réplicas, use afinidade de sessão ou coordenação compartilhada. Nunca use a chave fictícia dos testes em produção.

## Funcionalidades

- Visão geral com totais reais, vagas recentes, candidaturas e evolução mensal.
- Vagas com filtro/paginação no servidor, rascunhos, etapas, critérios, horas e transições de status.
- Candidatos com busca, ficha, cadastro e atualização com base legal e data de coleta.
- Candidaturas em painel ou lista: inscrição, notas, movimentação, decisão, avaliadores, proposta e contratação.
- Custos calculados pela API, referência de vigência e ajustes pela coordenação.
- Indicadores com período de até 36 meses, recortes e exportação CSV com neutralização de fórmulas.
- Cadastros de áreas, cargos e canais; parâmetros e metas com vigência; visitas mensais.
- Administração de contas, perfis, bloqueio e vínculo UUID Supabase.
- Privacidade, solicitações de titular, exportação autorizada, conclusão confirmada e auditoria.
- Minha conta e alteração de senha.

A nota final da API é normalizada entre 0 e 1 e apresentada como percentual. As notas por critério continuam entre 0 e 5. O painel de candidaturas agrupa a página atual; filtros e paginação percorrem os demais registros.

A ficha de candidatura inclui etapas e critérios após a mesma verificação de acesso existente. Avaliadores designados recebem esses metadados para avaliar, sem acessar propostas ou custos.

## Testes

```powershell
npm run format:check
npm run typecheck
npm test
npm run build
npx playwright install chromium
npm run test:e2e
npm audit --omit=dev --audit-level=high
```

Vitest verifica criptografia, cookies adulterados, origem, caminhos, redirecionamento, perfis, status, formatos e CSV. Playwright executa o aplicativo compilado em desktop e mobile, cobrindo autenticação, cadastros, filtros, paginação, permissões, renovação, CSRF, indisponibilidade e logout.

`tests/fixtures/backend.mjs` é **sintético e exclusivo dos testes**: não acessa Supabase, não carrega segredos e não integra a imagem final. A aplicação normal nunca importa fixtures. Verificações adicionais com serviços reais são registradas nas evidências locais ignoradas pelo Git.

## Produção e Docker

```powershell
npm run build
npm start
# Alternativamente, com a .env preenchida:
docker compose up --build -d
```

`start` lê a `.env` se existir e também aceita variáveis do ambiente. No container, `host.docker.internal:8080` aponta para o backend na máquina hospedeira; use `DOCKER_BACKEND_BASE_URL` para outra rede.

A imagem final contém somente `.output`, executa sem root e usa filesystem somente leitura no Compose. Segredos, dependências locais, fixtures e relatórios são excluídos do contexto Docker. A porta do Compose fica vinculada ao loopback; configure HTTPS no proxy público.

Referências: [Nuxt](https://nuxt.com/docs/4.x), [Vue](https://vuejs.org/guide/introduction.html), [Playwright](https://playwright.dev/docs/intro), [Vitest](https://vitest.dev/guide/).

## Identidade visual

As imagens originais da marca ficam em `public/branding/`, sem alteração de pixels. O componente `SirsMarca` usa uma janela SVG para enquadrar a logo horizontal no login e na navegação e o símbolo na página de erro, preservando as proporções e o texto da imagem. O favicon PNG utiliza o símbolo sobre fundo transparente e está configurado no cabeçalho global. Os demais ícones continuam na biblioteca Lucide.

## Organização visual

`app/assets/css/main.css` reúne os elementos compartilhados de tabelas, formulários, estados e modais. `workspace.css` define a navegação, o resumo operacional, o login e seus pontos de adaptação. A navegação usa superfícies claras e realce discreto; os indicadores formam uma faixa, e as vagas têm prioridade sobre o histórico no painel. O login apresenta o fluxo do recrutamento sem simular dados ou atividades da conta, com controle acessível de visibilidade da senha e aviso de Caps Lock.

Os testes de navegador conferem a disposição em desktop/mobile, as logos, os controles do login e a ausência de transbordamento nas larguras de 320, 768 e 1024 pixels.

### Profundidade visual e indicadores

A identidade usa índigo e superfícies em camadas, mantendo uma única faixa de indicadores em vez de cartões repetidos. O painel lateral `SirsResumoPeriodo` apresenta os dados já retornados pela API: percentual no prazo, meta vigente, ciclo de admissão e vagas atrasadas. A ausência de vagas consideradas recebe um estado próprio, sem simular resultados.

`SirsGraficoHistorico` é compartilhado entre o painel e os indicadores, com escala calculada a partir dos valores, meses identificados, linhas de referência e tabela equivalente para leitores de tela. Percentuais visuais são limitados entre 0 e 100; os valores de origem permanecem inalterados.

## Mensagens do sistema

`useNotificacoes().adicionar(texto, tipo, tituloOpcional)` aceita `informacao`, `sucesso`, `erro` e `alerta`. Os avisos aparecem sem bloquear a tela (`SirsMensagem`), no canto superior direito ou junto aos campos de login. Mensagens idênticas pendentes não são duplicadas. Os ícones são Lucide, e o conteúdo é sempre texto, sem HTML recebido da API.

As mensagens não usam diálogo modal nem alteram o foco. Cada aviso tem um botão de fechar e um estado acessível (`alert` para erros e alertas, `status` para informações e sucesso).

Salvamentos, avaliações e demais notificações existentes usam o novo componente. Falhas de login, sessão expirada, erros do formulário e a explicação de permissões em Minha conta também foram integrados. Os erros de campos continuam disponíveis no formulário após fechar o aviso, sem perder os valores digitados. Se a gravação funcionar e apenas a atualização da lista falhar, o sistema informa que os dados já foram salvos.

O login confirmado exibe “Login realizado” depois do redirecionamento ao destino solicitado. O logout confirmado exibe “Sessão encerrada” na tela de acesso. Falhas seguem o fluxo de erro, sem emitir confirmação de sucesso. As mensagens não reaparecem ao recarregar a página.

### Mensagens sem interrupção

Erros, alertas e confirmações aparecem no canto superior direito, sem bloquear a navegação nem mudar o foco. Na tela de login, ficam junto aos campos. Avisos de sucesso no canto desaparecem após oito segundos, pausando enquanto o ponteiro ou foco está no aviso; erros e alertas permanecem até serem fechados.
