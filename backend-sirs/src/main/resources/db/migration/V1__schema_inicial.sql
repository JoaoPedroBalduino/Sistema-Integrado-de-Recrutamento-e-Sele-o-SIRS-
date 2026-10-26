-- CAD - Cadastros e Parametros

CREATE TABLE area (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome   VARCHAR(100) NOT NULL UNIQUE,
    ativo  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE cargo (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome   VARCHAR(100) NOT NULL UNIQUE,
    ativo  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE canal (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome   VARCHAR(100) NOT NULL UNIQUE,
    ativo  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE parametro_custo (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chave            VARCHAR(100) NOT NULL,
    valor            NUMERIC(14,4) NOT NULL,
    vigencia_inicio  DATE NOT NULL,
    vigencia_fim     DATE,
    UNIQUE (chave, vigencia_inicio)
);

CREATE TABLE meta (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    indicador        VARCHAR(100) NOT NULL,
    valor            NUMERIC(14,4) NOT NULL,
    vigencia_inicio  DATE NOT NULL,
    vigencia_fim     DATE,
    UNIQUE (indicador, vigencia_inicio)
);

-- SEG - Acesso e Conformidade
CREATE TABLE usuario (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome              VARCHAR(150) NOT NULL,
    email             VARCHAR(255) NOT NULL UNIQUE,
    senha_hash        VARCHAR(255) NOT NULL,
    perfil            VARCHAR(20)  NOT NULL
        CHECK (perfil IN ('ADMIN', 'COORD_RH', 'RECRUTADOR', 'GESTOR', 'AVALIADOR', 'DIRETORIA')),
    ativo             BOOLEAN NOT NULL DEFAULT TRUE,
    tentativas_falhas INTEGER NOT NULL DEFAULT 0,
    bloqueado         BOOLEAN NOT NULL DEFAULT FALSE
);

-- VAG - Gestao de Vagas
CREATE TABLE vaga (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titulo                    VARCHAR(200) NOT NULL,
    cargo_id                  BIGINT NOT NULL REFERENCES cargo (id),
    area_id                   BIGINT NOT NULL REFERENCES area (id),
    solicitante_id            BIGINT NOT NULL REFERENCES usuario (id),
    recrutador_id             BIGINT NOT NULL REFERENCES usuario (id),
    posicoes                  INTEGER NOT NULL CHECK (posicoes > 0),
    cidade                    VARCHAR(100),
    estado                    VARCHAR(2),
    remoto                    BOOLEAN NOT NULL DEFAULT FALSE,
    observacoes               TEXT,
    status                    VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO'
        CHECK (status IN ('RASCUNHO', 'EM_APROVACAO', 'PUBLICADA', 'CONGELADA', 'FECHADA')),
    data_abertura             DATE,
    data_publicacao           DATE,
    data_fechamento           DATE,
    prazo_alvo_dias           INTEGER,
    investimento_divulgacao   NUMERIC(12,2) NOT NULL DEFAULT 0,
    CHECK (data_fechamento IS NULL OR data_publicacao IS NULL OR data_fechamento >= data_publicacao)
);

CREATE TABLE vaga_canal (
    vaga_id  BIGINT NOT NULL REFERENCES vaga (id),
    canal_id BIGINT NOT NULL REFERENCES canal (id),
    PRIMARY KEY (vaga_id, canal_id)
);

-- Historico do ciclo de vida da vaga
CREATE TABLE historico_status_vaga (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vaga_id         BIGINT NOT NULL REFERENCES vaga (id),
    status_anterior VARCHAR(20),
    status_novo     VARCHAR(20) NOT NULL,
    usuario_id      BIGINT NOT NULL REFERENCES usuario (id),
    alterado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Etapas do processo seletivo por vaga, sem limite fixo 
CREATE TABLE etapa_vaga (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vaga_id    BIGINT NOT NULL REFERENCES vaga (id),
    nome       VARCHAR(150) NOT NULL,
    descricao  TEXT,
    ordem      INTEGER NOT NULL,
    UNIQUE (vaga_id, ordem)
);

-- Criterios de avaliacao por vaga, sem limite fixo
CREATE TABLE criterio_vaga (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vaga_id    BIGINT NOT NULL REFERENCES vaga (id),
    nome       VARCHAR(150) NOT NULL,
    descricao  TEXT,
    peso       SMALLINT NOT NULL CHECK (peso BETWEEN 1 AND 5)
);

-- Volumes/parametros de custo ajustados por vaga, padrao em parametro_custo
CREATE TABLE vaga_parametro_custo (
    vaga_id BIGINT NOT NULL REFERENCES vaga (id),
    chave   VARCHAR(100) NOT NULL,
    valor   NUMERIC(14,4) NOT NULL,
    PRIMARY KEY (vaga_id, chave)
);

-- Apontamento de horas por vaga, usado em RN-12
CREATE TABLE apontamento_horas (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vaga_id    BIGINT NOT NULL REFERENCES vaga (id),
    usuario_id BIGINT NOT NULL REFERENCES usuario (id),
    data       DATE NOT NULL,
    horas      NUMERIC(4,2) NOT NULL CHECK (horas > 0 AND horas <= 24),
    atividade  VARCHAR(200)
);

-- TRI - Triagem e Avaliacao
CREATE TABLE candidato (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                VARCHAR(150) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    curriculo_url       VARCHAR(500),
    base_legal          VARCHAR(50),
    consentimento_em    TIMESTAMPTZ,
    consentimento_versao VARCHAR(20),
    anonimizado_em      TIMESTAMPTZ
);

CREATE TABLE candidatura (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vaga_id         BIGINT NOT NULL REFERENCES vaga (id),
    candidato_id    BIGINT NOT NULL REFERENCES candidato (id),
    canal_id        BIGINT NOT NULL REFERENCES canal (id),
    etapa_atual_id  BIGINT REFERENCES etapa_vaga (id),
    status          VARCHAR(20) NOT NULL DEFAULT 'EM_AVALIACAO'
        CHECK (status IN ('EM_AVALIACAO', 'APROVADO', 'REPROVADO')),
    nota_final      NUMERIC(5,4),
    contratada_em   TIMESTAMPTZ,
    UNIQUE (vaga_id, candidato_id)
);

-- Historico de movimentacao entre etapas, base do funil
CREATE TABLE historico_etapa (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidatura_id  BIGINT NOT NULL REFERENCES candidatura (id),
    etapa_id        BIGINT NOT NULL REFERENCES etapa_vaga (id),
    usuario_id      BIGINT NOT NULL REFERENCES usuario (id),
    movido_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Notas por criterio, base da nota final
CREATE TABLE avaliacao_criterio (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidatura_id  BIGINT NOT NULL REFERENCES candidatura (id),
    criterio_id     BIGINT NOT NULL REFERENCES criterio_vaga (id),
    avaliador_id    BIGINT NOT NULL REFERENCES usuario (id),
    nota            SMALLINT NOT NULL CHECK (nota BETWEEN 0 AND 5),
    UNIQUE (candidatura_id, criterio_id)
);

-- Avaliadores designados para uma candidatura
CREATE TABLE designacao_avaliador (
    candidatura_id BIGINT NOT NULL REFERENCES candidatura (id),
    usuario_id     BIGINT NOT NULL REFERENCES usuario (id),
    PRIMARY KEY (candidatura_id, usuario_id)
);

-- ADM - Proposta
CREATE TABLE proposta (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidatura_id  BIGINT NOT NULL UNIQUE REFERENCES candidatura (id),
    salario_ofertado NUMERIC(12,2) NOT NULL,
    data_proposta   DATE NOT NULL,
    data_aceite     DATE
);

-- IND - Indicadores
-- Visitas por canal e periodo (RF-IND-16), usado em RN-19
CREATE TABLE visitas_canal (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    canal_id BIGINT NOT NULL REFERENCES canal (id),
    periodo  DATE NOT NULL, -- primeiro dia do mes
    visitas  INTEGER NOT NULL CHECK (visitas >= 0),
    UNIQUE (canal_id, periodo)
);

-- SEG - Auditoria e LGPD
CREATE TABLE auditoria (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id    BIGINT REFERENCES usuario (id),
    entidade      VARCHAR(100) NOT NULL,
    entidade_id   BIGINT,
    acao          VARCHAR(20) NOT NULL
        CHECK (acao IN ('CRIACAO', 'ALTERACAO', 'EXCLUSAO', 'CONSULTA', 'ANONIMIZACAO')),
    dados_antes   JSONB,
    dados_depois  JSONB,
    ocorrida_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- Indices sugeridos para RNF-DES-01

CREATE INDEX idx_candidatura_vaga_etapa_status ON candidatura (vaga_id, etapa_atual_id, status);
CREATE INDEX idx_historico_etapa_candidatura ON historico_etapa (candidatura_id);
CREATE INDEX idx_vaga_status_recrutador_abertura ON vaga (status, recrutador_id, data_abertura);
CREATE INDEX idx_avaliacao_criterio_candidatura ON avaliacao_criterio (candidatura_id);
