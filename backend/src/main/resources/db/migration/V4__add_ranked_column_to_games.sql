-- V4__add_ranked_column_to_games.sql
-- Adiciona coluna ranked à tabela games para diferenciar partidas que contam para stats/elo

ALTER TABLE games ADD COLUMN ranked BOOLEAN NOT NULL DEFAULT false;

-- Backfill: partidas originadas de matchmaking (private_room = false) são ranqueadas
UPDATE games SET ranked = true WHERE private_room = false;
