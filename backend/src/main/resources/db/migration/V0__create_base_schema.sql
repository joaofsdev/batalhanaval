-- V0__create_base_schema.sql
-- Schema base completo para banco novo

-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    elo_rating INTEGER NOT NULL DEFAULT 1000,
    suspended_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Games
CREATE TABLE games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player1_id UUID NOT NULL REFERENCES users(id),
    player2_id UUID REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    game_mode VARCHAR(20) NOT NULL DEFAULT 'CLASSIC',
    room_token VARCHAR(255) UNIQUE,
    private_room BOOLEAN NOT NULL DEFAULT false,
    ranked BOOLEAN NOT NULL DEFAULT false,
    player1_ready BOOLEAN NOT NULL DEFAULT false,
    player2_ready BOOLEAN NOT NULL DEFAULT false,
    current_turn_number INTEGER NOT NULL DEFAULT 1,
    next_storm_turn INTEGER NOT NULL DEFAULT 3,
    next_ability_rotation_turn INTEGER NOT NULL DEFAULT 4,
    fog_active BOOLEAN NOT NULL DEFAULT false,
    bonus_shot BOOLEAN NOT NULL DEFAULT false,
    current_turn_id UUID REFERENCES users(id),
    winner_id UUID REFERENCES users(id),
    player1_elo_before INTEGER,
    player2_elo_before INTEGER,
    consecutive_skips INTEGER NOT NULL DEFAULT 0,
    placement_deadline TIMESTAMP,
    cancellation_reason VARCHAR(30),
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_game_status ON games(status);
CREATE INDEX idx_game_player1 ON games(player1_id);
CREATE INDEX idx_game_player2 ON games(player2_id);

-- Boards
CREATE TABLE boards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES games(id),
    owner_id UUID NOT NULL REFERENCES users(id),
    ready BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_board_game_owner UNIQUE (game_id, owner_id)
);

-- Ships
CREATE TABLE ships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL REFERENCES boards(id),
    ship_type VARCHAR(30) NOT NULL,
    origin_row INTEGER NOT NULL,
    origin_col INTEGER NOT NULL,
    orientation VARCHAR(10) NOT NULL,
    hits INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_ship_board ON ships(board_id);

-- Cells
CREATE TABLE cells (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL REFERENCES boards(id),
    cell_row INTEGER NOT NULL,
    cell_col INTEGER NOT NULL,
    has_ship BOOLEAN NOT NULL DEFAULT false,
    hit BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_cell_board_row_col UNIQUE (board_id, cell_row, cell_col)
);

CREATE INDEX idx_cell_board ON cells(board_id);

-- Shots
CREATE TABLE shots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES games(id),
    attacker_id UUID NOT NULL REFERENCES users(id),
    target_board_id UUID NOT NULL REFERENCES boards(id),
    shot_row INTEGER NOT NULL,
    shot_col INTEGER NOT NULL,
    result VARCHAR(10) NOT NULL,
    sunk_ship_type VARCHAR(30),
    sunk_ship_origin_row INTEGER,
    sunk_ship_origin_col INTEGER,
    sunk_ship_orientation VARCHAR(10),
    fired_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_shot_board_row_col UNIQUE (target_board_id, shot_row, shot_col)
);

CREATE INDEX idx_shot_game ON shots(game_id);
CREATE INDEX idx_shot_attacker ON shots(attacker_id);

-- Storm Events
CREATE TABLE storm_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES games(id),
    turn_number INTEGER NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    affected_axis VARCHAR(50),
    resolved BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_storm_game ON storm_events(game_id);
CREATE INDEX idx_storm_game_turn ON storm_events(game_id, turn_number);

-- Player Abilities
CREATE TABLE player_abilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES games(id),
    user_id UUID NOT NULL REFERENCES users(id),
    ability_type VARCHAR(30) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    used_on_turn INTEGER,
    CONSTRAINT uk_ability_game_user UNIQUE (game_id, user_id)
);

CREATE INDEX idx_ability_game ON player_abilities(game_id);

-- Admin Audit Log
CREATE TABLE admin_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id UUID NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_admin_id ON admin_audit_log(admin_id);
CREATE INDEX idx_audit_log_created_at ON admin_audit_log(created_at DESC);
CREATE INDEX idx_audit_log_target ON admin_audit_log(target_type, target_id);
