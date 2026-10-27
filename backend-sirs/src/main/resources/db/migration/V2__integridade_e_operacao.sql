ALTER TABLE usuario ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE vaga ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE candidato ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE candidato ADD COLUMN criado_em TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE candidato ADD COLUMN ultima_atividade_em TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE candidatura ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE candidatura ADD COLUMN criada_em TIMESTAMPTZ NOT NULL DEFAULT now();
CREATE UNIQUE INDEX uk_usuario_email_normalizado ON usuario(lower(trim(email)));
CREATE UNIQUE INDEX uk_candidato_email_normalizado ON candidato(lower(trim(email)));
ALTER TABLE parametro_custo ADD CONSTRAINT ck_parametro_valor CHECK (valor >= 0);
ALTER TABLE parametro_custo ADD CONSTRAINT ck_parametro_vigencia CHECK (vigencia_fim IS NULL OR vigencia_fim >= vigencia_inicio);
ALTER TABLE meta ADD CONSTRAINT ck_meta_valor CHECK (valor >= 0);
ALTER TABLE meta ADD CONSTRAINT ck_meta_vigencia CHECK (vigencia_fim IS NULL OR vigencia_fim >= vigencia_inicio);
ALTER TABLE vaga ADD CONSTRAINT ck_vaga_investimento CHECK (investimento_divulgacao >= 0);
ALTER TABLE vaga ADD CONSTRAINT ck_vaga_prazo CHECK (prazo_alvo_dias IS NULL OR prazo_alvo_dias > 0);
ALTER TABLE etapa_vaga ADD CONSTRAINT ck_etapa_ordem CHECK (ordem > 0);
ALTER TABLE candidatura ADD CONSTRAINT ck_nota_final CHECK (nota_final BETWEEN 0 AND 1);
ALTER TABLE proposta ADD CONSTRAINT ck_proposta_salario CHECK (salario_ofertado > 0);
ALTER TABLE proposta ADD CONSTRAINT ck_proposta_datas CHECK (data_aceite IS NULL OR data_aceite >= data_proposta);
ALTER TABLE visitas_canal ADD CONSTRAINT ck_visitas_mes CHECK (EXTRACT(DAY FROM periodo) = 1);
CREATE INDEX idx_candidatura_candidato ON candidatura(candidato_id);
CREATE INDEX idx_apontamento_vaga ON apontamento_horas(vaga_id);
CREATE INDEX idx_candidato_retencao ON candidato(ultima_atividade_em) WHERE anonimizado_em IS NULL;
CREATE INDEX idx_auditoria_ocorrida ON auditoria(ocorrida_em DESC);
CREATE TABLE solicitacao_titular (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidato_id BIGINT NOT NULL REFERENCES candidato(id),
    tipo VARCHAR(20) NOT NULL CHECK(tipo IN ('ACESSO','CORRECAO','ANONIMIZACAO')),
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTA' CHECK(status IN ('ABERTA','CONCLUIDA')),
    solicitada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    prazo DATE NOT NULL,
    concluida_em TIMESTAMPTZ
);
CREATE INDEX idx_solicitacao_status ON solicitacao_titular(status,prazo);
