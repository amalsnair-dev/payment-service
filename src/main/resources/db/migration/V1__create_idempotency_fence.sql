CREATE TABLE idempotency_fence(
    idempotency_key VARCHAR(255) PRIMARY KEY ,
    fencing_token BIGINT NOT NULL
);