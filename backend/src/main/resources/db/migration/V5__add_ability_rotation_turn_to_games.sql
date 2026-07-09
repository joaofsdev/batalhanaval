-- V5__add_ability_rotation_turn_to_games.sql
-- Adiciona coluna para controlar quando ocorre a próxima rotação de habilidades no Storm Mode

ALTER TABLE games ADD COLUMN next_ability_rotation_turn INT NOT NULL DEFAULT 4;
