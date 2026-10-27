# SIRS — backend

API REST em **Java 17, Spring Boot 4.1.2 e Lombok**, organizada em MVC e baseada nas telas de `prototipo-sirs` e no esquema PostgreSQL existente.

## Arquitetura MVC

```text
br.com.sirs.api
├── controller       # HTTP, validacao de entrada e codigos de resposta
├── service          # Regras de negocio e limites transacionais
├── repository       # Persistencia JPA e consultas SQL de indicadores
├── model
│   ├── entity       # Entidades persistentes; nunca retornadas pelos controllers
│   ├── enums        # Dominios e estados permitidos
│   ├── request      # Contratos de entrada com nomes completos
│   ├── response     # Contratos de saida, separados das entidades
│   └── value        # Valores usados nos calculos de negocio
├── security         # Perfis e autorizacao por vaga/designacao
├── config           # Seguranca, documentacao, paginacao e inicializacao
├── exception        # Respostas padronizadas application/problem+json
└── scheduler        # Rotina diaria de retencao
```

Os controllers delegam aos servicos. Os servicos verificam permissoes e regras antes de acessar os repositorios. Lombok gera construtores de injecao e acessores das entidades, sem `@Data`, `toString` de dados pessoais ou serializacao de relacionamentos JPA. Requisicoes e respostas usam records imutaveis, cada um em seu proprio arquivo. Um teste verifica a separacao entre controller e persistencia.

## Executar com Docker

1. Copie `.env.example` para `.env` dentro desta pasta.
2. Preencha `DB_PASSWORD`, `JWT_SECRET` e `ADMIN_PASSWORD`. A chave JWT deve conter pelo menos 32 bytes; a senha inicial deve ter pelo menos 12 caracteres e no maximo 72 bytes UTF-8.
3. Execute:

```powershell
docker compose up --build -d
```

O administrador inicial e criado apenas quando `ADMIN_EMAIL` esta preenchido e ainda nao existe um administrador. Nao ha contas ou senhas padrao. Depois da primeira inicializacao, remova `ADMIN_PASSWORD` do ambiente de execucao.

- API: `http://localhost:8080/api/v1`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Saude: `http://localhost:8080/actuator/health`

## Executar diretamente no Windows

Requisitos: JDK 17 ou superior e PostgreSQL. O Maven Wrapper ja acompanha o projeto. Um arquivo `.env` nao e carregado automaticamente ao executar Java fora do Compose.

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/sirs'
$env:DB_USERNAME = 'sirs'
$env:DB_PASSWORD = Read-Host 'Senha do banco'
$env:JWT_SECRET = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$env:ADMIN_EMAIL = 'administrador@example.com'
$env:ADMIN_PASSWORD = Read-Host 'Senha inicial do administrador'
.\mvnw.cmd spring-boot:run
```

No Linux/macOS, exporte as mesmas variaveis e use `./mvnw`. Mantenha a chave JWT estavel entre reinicializacoes se os tokens emitidos precisarem continuar validos. Em implantacao, configure as origens reais em `CORS_ORIGINS`, HTTPS no ponto de entrada e `API_DOCS_ENABLED=false` quando a documentacao nao deva estar publicada.

## Swagger / OpenAPI

A documentação interativa está disponível em `http://localhost:8080/swagger-ui.html`
(também funciona `/swagger-ui/index.html`). A especificação pode ser consumida em:

- JSON: `http://localhost:8080/v3/api-docs`
- YAML: `http://localhost:8080/v3/api-docs.yaml`

A interface organiza as **59 operações em 13 módulos**, com descrições das regras,
parâmetros de paginação, formatos de entrada, códigos HTTP e erros padronizados.
Apenas as rotas `/api/v1/**` entram na especificação; endpoints internos de monitoramento
não são publicados como operações de negócio.

### Testar uma chamada autenticada

1. Abra o módulo **Autenticação** e a operação `POST /api/v1/auth/login`.
2. Clique em **Try it out**, informe o e-mail e a senha de uma conta cadastrada e execute.
3. Copie somente o valor de `accessToken` retornado na resposta.
4. Clique em **Authorize** e cole esse valor, **sem escrever o prefixo `Bearer`**.
5. Execute a operação desejada. As permissões por perfil, vaga e designação continuam valendo.

O login e os arquivos da documentação são públicos; os recursos da API continuam exigindo JWT.
A interface não persiste a autorização após recarregar a página, e os campos de senha são
marcados como entrada exclusiva (`writeOnly`) no contrato OpenAPI.

Para desativar tanto a interface quanto as especificações, configure
`API_DOCS_ENABLED=false` e reinicie a aplicação. As URLs documentadas retornarão 404,
sem alterar a autenticação da API. A porta dos exemplos acompanha a variável `PORT`.

Os testes integrados verificam a interface, os recursos estáticos, JSON/YAML, o contrato
JWT, o login público, todas as operações, os erros, a paginação e a desativação da documentação.

## Autenticacao

```http
POST /api/v1/auth/login
Content-Type: application/json

{"email":"administrador@example.com","senha":"SENHA_CONFIGURADA_NO_AMBIENTE"}
```

Use o campo `accessToken` como `Authorization: Bearer TOKEN` nas demais chamadas. Tokens duram 30 minutos. Cinco falhas consecutivas bloqueiam a conta. O administrador pode desbloquear pelo cadastro de usuarios. Desativacao, alteracao de perfil, troca de senha e logout invalidam imediatamente os tokens anteriores; o logout encerra todas as sessoes da conta.

`GET /auth/me` consulta o usuario autenticado; `PUT /auth/senha` recebe `senhaAtual` e `novaSenha`. Apenas login, saude e documentacao sao publicos.

## Rotas principais

Todas as rotas abaixo usam o prefixo `/api/v1`. Os contratos completos e campos obrigatorios estao no Swagger.

| Modulo | Rotas |
|---|---|
| Usuarios | `GET/POST /usuarios`, `PUT /usuarios/{identificador}` |
| Cadastros | `GET/POST /cadastros/{areas,cargos,canais}`, `PUT /cadastros/{tipo}/{identificador}` |
| Responsaveis | `GET /cadastros/recrutadores`, `GET /cadastros/solicitantes` |
| Vagas | `GET/POST /vagas`, `GET/PUT /vagas/{identificador}`, `PATCH /vagas/{identificador}/status` |
| Estrutura da vaga | `POST /vagas/{identificador}/etapas` e `/criterios`; `PUT/DELETE` nos respectivos identificadores |
| Horas | `POST /vagas/{identificador}/horas`; a consulta detalhada da vaga inclui os apontamentos |
| Candidatos | `GET/POST /candidatos`, `GET/PUT /candidatos/{identificador}` |
| Candidaturas | `GET/POST /candidaturas`, `GET /candidaturas/{identificador}` |
| Selecao | `PATCH /candidaturas/{identificador}/etapa` e `/status`; `PUT /candidaturas/{identificador}/avaliacoes` |
| Avaliadores | `POST /candidaturas/{identificador}/avaliadores`, `DELETE /candidaturas/{identificador}/avaliadores/{usuarioId}` |
| Admissao | `POST /candidaturas/{identificador}/proposta` e `/contratacao` |
| Custos | `GET /vagas/{vagaId}/custos`, `PUT /vagas/{vagaId}/custos/parametros` |
| Configuracoes | `GET/POST /configuracoes/parametros-custo` e `/metas`; `PATCH /configuracoes/{tipo}/{identificador}/vigencia` |
| Visitas | `GET/PUT /visitas-canais` |
| Indicadores | `GET /indicadores/{resumo,historico,cargos,recrutadores,canais}?de=AAAA-MM-DD&ate=AAAA-MM-DD`; `GET /indicadores/vagas/{vagaId}` |
| Privacidade | `GET/POST /privacidade/solicitacoes`; `POST /privacidade/solicitacoes/{identificador}/conclusao`; `GET /privacidade/candidatos/{candidatoId}/exportacao` |
| Auditoria | `GET /auditoria` |

Listagens operacionais aceitam `page`, `size` (maximo 100) e campos de `sort` permitidos para cada recurso. A resposta contem `content`, `paginacao` (indice iniciado em zero), `size`, `totalElements` e `totalPages`. Vagas filtram por status, area, cargo, recrutador e intervalo de abertura. Candidaturas filtram por vaga, etapa e status; a ordenacao padrao e por nota final decrescente. Candidatos aceitam `pesquisa` por nome ou email.

## Fluxo de uso

1. O administrador cadastra usuarios, areas, cargos e canais. A coordenacao ou o recrutador cadastra candidatos com base legal, data de coleta e, quando aplicavel, versao do consentimento.
2. A coordenacao, o recrutador responsavel ou o gestor solicitante cadastra a vaga em rascunho, etapas e criterios.
3. O responsavel envia para `EM_APROVACAO`. A coordenacao ou diretoria publica ou devolve para rascunho.
4. Somente uma vaga `PUBLICADA` recebe candidaturas. A inscricao inicia na primeira etapa e registra o historico.
5. O responsavel movimenta e decide; avaliadores designados podem consultar e atribuir notas, mas nao decidir, movimentar, contratar nem visualizar a proposta salarial.
6. A proposta exige aprovacao na ultima etapa. A contratacao recebe `dataAceite`, exige proposta e respeita o total de posicoes, inclusive em chamadas concorrentes.
7. A vaga pode ser congelada, retomada ou fechada conforme o ciclo permitido. Uma vaga fechada nao reabre.

Exemplo de criacao de vaga (substitua os identificadores pelos registros cadastrados):

```json
{
  "titulo": "Desenvolvedor Backend",
  "cargoId": 1,
  "areaId": 1,
  "solicitanteId": 4,
  "recrutadorId": 3,
  "posicoes": 2,
  "remoto": true,
  "prazoAlvoDias": 30,
  "investimentoDivulgacao": 10.00,
  "canalIds": [1]
}
```

## Permissoes

A matriz do prototipo foi mantida: administrador administra usuarios, privacidade, parametros e listas, mas tem somente leitura dos processos de recrutamento; coordenacao opera os processos; recrutador altera suas vagas; gestor consulta e altera apenas suas vagas; avaliador acessa somente candidaturas designadas; diretoria consulta vagas/indicadores/custos e aprova vagas. A diretoria nao acessa candidatos ou candidaturas. Nome e identificador dos responsaveis ativos ficam disponiveis para os formularios sem expor senhas ou dados internos da conta.

## Regras e decisoes do prototipo

- **RN-04:** soma de nota vezes peso dividida por cinco vezes a soma dos pesos; criterios sem nota contam como zero. Soma zero resulta em nota final ausente, excluida da media.
- **RN-06:** funil calculado da ultima para a primeira etapa, sem limite fixo de etapas; nao presume que toda movimentacao foi sequencial.
- **RN-01/02:** salarios por papel, fator e horas mensais; calculos com `BigDecimal`, sem arredondar antecipadamente o valor-hora. Os valores de referencia geram R$ 513,86 em horas e R$ 573,86 no total.
- **PA-07:** a composicao usa kit e divulgacao, nao o campo "Outros investimentos", conforme a opcao apresentada no prototipo. `investimentoRealizado=true` substitui a verba padrao pelo investimento efetivo da vaga.
- **Vigencias:** intervalos inclusivos; sobreposicoes sao rejeitadas e serializadas por chave. Encerre a vigencia anterior antes de cadastrar a seguinte. Metas percentuais usam escala de 0 a 100.
- **RN-12/13/14:** horas e custos de vagas fechadas no intervalo; vagas no prazo usam o intervalo de abertura, excluem congeladas e comparam abertura com fechamento ou referencia final do periodo.
- **RN-19:** visitas sao mensais, registradas no primeiro dia do mes. Filtros de canal incluem os meses tocados pelo intervalo; contratacoes usam as datas exatas. Visitas zero produzem percentual zero.
- **RN-25:** historico mensal limitado a 36 meses; a meta vigente e a do fim de cada subperiodo. Os relatorios recalculam os dados atuais filtrados por data, nao constituem snapshots imutaveis do estado antigo das vagas.
- **Privacidade:** solicitacoes recebem prazo de 15 dias. Correcao de dados e feita no cadastro antes da conclusao do pedido. A rotina diaria processa ate 500 candidatos por execucao, apos 12 meses sem atividade e sem candidaturas ativas ou contratacoes. A desidentificacao remove nome original, email, curriculo e dados de consentimento, mantendo referencias estatisticas. A trilha registra usuario, entidade, acao e instante, sem copiar senhas, tokens ou dados pessoais para JSON de auditoria.

Etapas, criterios e dados estruturais da vaga sao editaveis em rascunho; depois do envio para aprovacao ficam preservados. Reprovacao e final. Reversao de reprovacao e reabertura de vaga ficaram desabilitadas, pois dependem das decisoes pendentes no documento de requisitos. A pagina HTML continua sendo um prototipo estatico; esta entrega implementa a API, sem modificar as telas para consumir os endpoints.

## Migrations

A migration `V1__schema_inicial.sql` foi preservada. `V2` acrescenta versoes, integridade, indices, datas e solicitacoes de titular. `V3` cadastra parametros de referencia e metas, sem usuarios nem segredos. O Flyway executa as migrations e o Hibernate apenas valida o esquema (`ddl-auto=validate`).

## Testes e padronizacao

```powershell
.\mvnw.cmd test
.\mvnw.cmd spotless:apply
```

Os testes unitarios independem de banco. Para executar tambem os testes HTTP, autorizacao, migrations e concorrencia, crie um banco **exclusivo de testes** chamado `sirs_test` e configure:

```powershell
$env:SIRS_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/sirs_test'
$env:SIRS_TEST_DB_USERNAME = 'sirs_test'
$env:SIRS_TEST_DB_PASSWORD = 'SENHA_DO_BANCO_DE_TESTES'
.\mvnw.cmd verify
```

A suite integrada limpa as tabelas desse banco entre cenarios. Sem `SIRS_TEST_DB_URL`, os testes integrados ficam explicitamente ignorados. O pipeline de integracao continua prepara PostgreSQL e executa todos. `verify` tambem verifica formatacao e gera o relatorio JaCoCo em `target/site/jacoco/index.html`.

As respostas de erro seguem `application/problem+json`, com identificador de requisicao e erros por campo quando aplicavel. Codigos principais: 400 entrada invalida, 401 autenticacao, 403 autorizacao, 404 recurso inexistente, 409 conflito de unicidade/concorrencia e 422 regra de negocio.

Referencias de compatibilidade: [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [Springdoc](https://springdoc.org/).
