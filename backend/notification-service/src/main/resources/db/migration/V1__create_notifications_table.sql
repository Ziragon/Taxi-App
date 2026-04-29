CREATE SEQUENCE IF NOT EXISTS notifications_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE notifications
(
    id             BIGINT      NOT NULL,
    trip_id        BIGINT,
    event_type     VARCHAR(32) NOT NULL,
    recipient_type VARCHAR(16) NOT NULL,
    recipient_id   BIGINT      NOT NULL,
    channel        VARCHAR(16) NOT NULL,
    message        TEXT        NOT NULL,
    status         VARCHAR(16) NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

CREATE INDEX idx_notifications_event_type ON notifications (event_type);

CREATE INDEX idx_notifications_recipient_id ON notifications (recipient_id);

CREATE INDEX idx_notifications_recipient_id_status ON notifications (recipient_id, status);

CREATE INDEX idx_notifications_status ON notifications (status);

CREATE INDEX idx_notifications_trip_id ON notifications (trip_id);