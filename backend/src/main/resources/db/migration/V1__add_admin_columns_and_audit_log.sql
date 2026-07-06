-- V1__add_admin_columns_and_audit_log.sql
-- Adiciona colunas de role/status à tabela users e cria tabela de auditoria admin

-- 1. Novas colunas na tabela users
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'PLAYER';
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN suspended_until TIMESTAMP;

-- Constraint para valores válidos de role
ALTER TABLE users ADD CONSTRAINT chk_user_role CHECK (role IN ('PLAYER', 'ADMIN'));

-- Constraint para valores válidos de status
ALTER TABLE users ADD CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'BANNED'));

-- 2. Tabela de auditoria de ações administrativas
CREATE TABLE admin_audit_log (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id   UUID         REFERENCES users(id),
    action     VARCHAR(50)  NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id  UUID         NOT NULL,
    details    TEXT,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

-- Índices para consultas comuns
CREATE INDEX idx_audit_log_admin_id ON admin_audit_log(admin_id);
CREATE INDEX idx_audit_log_created_at ON admin_audit_log(created_at DESC);
CREATE INDEX idx_audit_log_target ON admin_audit_log(target_type, target_id);
