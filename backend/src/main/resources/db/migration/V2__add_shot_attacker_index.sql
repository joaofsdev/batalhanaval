-- V2__add_shot_attacker_index.sql
-- Índice para queries de stats de combate por jogador (perfil)

CREATE INDEX idx_shot_attacker ON shots(attacker_id);
