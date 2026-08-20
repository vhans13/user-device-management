CREATE TABLE phone_numbers (
    id UUID PRIMARY KEY,
    number VARCHAR(20) NOT NULL,
    label VARCHAR(30) NOT NULL,
    user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_phone_numbers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_phone_numbers_user_id ON phone_numbers(user_id);

ALTER TABLE users ADD CONSTRAINT fk_users_preferred_phone
    FOREIGN KEY (preferred_phone_number_id) REFERENCES phone_numbers(id) ON DELETE SET NULL;
