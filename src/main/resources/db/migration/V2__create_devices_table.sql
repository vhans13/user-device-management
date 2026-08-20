CREATE TABLE devices (
    id UUID PRIMARY KEY,
    device_name VARCHAR(100) NOT NULL,
    device_model VARCHAR(50) NOT NULL,
    user_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_devices_user_id ON devices(user_id);
