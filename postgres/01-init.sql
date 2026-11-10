CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password TEXT,
    email TEXT NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL,
    provider_id TEXT UNIQUE,
    provider_type TEXT,
    picture TEXT,
    verification_code VARCHAR(255),
    verification_code_expiry TIMESTAMPTZ,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reset_token TEXT,
    reset_token_expiry TIMESTAMP,
    apple_refresh_token TEXT
);

CREATE TABLE jwt_token_blacklist (
    id BIGSERIAL PRIMARY KEY,
    jti VARCHAR(255) NOT NULL UNIQUE,
    username TEXT REFERENCES users(username) ON DELETE CASCADE,
    blacklisted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE item_posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    title VARCHAR(150) NOT NULL,
    description TEXT,
    category VARCHAR(50),

    post_type VARCHAR(20) NOT NULL CHECK (post_type IN ('LOST', 'FOUND')),

    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'PENDING_RESOLUTION', 'RESOLVED')),

    -- Resolution requires both the poster and the chosen counterparty to confirm.
    resolved_with_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    poster_confirmed_resolution BOOLEAN NOT NULL DEFAULT FALSE,
    counterparty_confirmed_resolution BOOLEAN NOT NULL DEFAULT FALSE,

    image_url TEXT,

    location GEOGRAPHY(POINT, 4326) NOT NULL,
    location_text TEXT,
    detail_location TEXT,

    reward_cents INTEGER CHECK (reward_cents IS NULL OR reward_cents >= 0),

    event_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_threads (
    id BIGSERIAL PRIMARY KEY,
    item_post_id BIGINT NOT NULL REFERENCES item_posts(id) ON DELETE CASCADE,
    user1_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT different_users CHECK (user1_id <> user2_id),
    CONSTRAINT ordered_users CHECK (user1_id < user2_id),
    CONSTRAINT unique_item_conversation UNIQUE (item_post_id, user1_id, user2_id)
);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    thread_id BIGINT NOT NULL REFERENCES chat_threads(id) ON DELETE CASCADE,
    sender_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_text TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE item_claims (
    id BIGSERIAL PRIMARY KEY,
    item_post_id BIGINT NOT NULL REFERENCES item_posts(id) ON DELETE CASCADE,
    claimant_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    chat_thread_id BIGINT REFERENCES chat_threads(id) ON DELETE SET NULL,

    status VARCHAR(20) NOT NULL CHECK (
        status IN ('PENDING', 'ACCEPTED', 'REJECTED')
    ),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_active_claim_per_user_per_item UNIQUE (item_post_id, claimant_user_id)
);

CREATE TABLE lost_searches (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    search_radius_meters DOUBLE PRECISION NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    used_timeline BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lost_searches_location ON lost_searches USING GIST (location);
CREATE INDEX idx_lost_searches_user_created ON lost_searches (user_id, created_at);

CREATE INDEX idx_item_posts_location
ON item_posts
USING GIST (location);
