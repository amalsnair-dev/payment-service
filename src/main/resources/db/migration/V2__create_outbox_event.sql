CREATE  TABLE outbox_event(
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregated_type VARCHAR(100) NOT NULL,
    aggregated_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30),
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP
);