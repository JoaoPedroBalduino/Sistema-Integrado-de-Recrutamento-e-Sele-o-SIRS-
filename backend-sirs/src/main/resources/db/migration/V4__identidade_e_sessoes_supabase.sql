-- O perfil e as permissoes continuam sob controle do SIRS, nunca de user_metadata.
ALTER TABLE usuario ADD COLUMN supabase_usuario_id UUID UNIQUE;
CREATE TABLE sessao_supabase (
    id UUID PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    criada_em TIMESTAMPTZ NOT NULL,
    revogada_em TIMESTAMPTZ
);
CREATE INDEX idx_sessao_supabase_usuario ON sessao_supabase(usuario_id) WHERE revogada_em IS NULL;
