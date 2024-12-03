CREATE TABLE Events (
    id UUID PRIMARY KEY NOT NULL,                               -- Unique identifier for the event
    stream_id UUID,                                            -- Identifier for the event stream
    causation_id UUID,                                         -- Identifier for the event that caused this event
    correlation_id UUID,                                       -- Identifier to correlate events from different streams
    type VARCHAR,                                              -- Type of the event (e.g., 'CharacterAdded', 'CharacterUpdated')
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,             -- Timestamp when the event was created or logged
    occurred_at TIMESTAMP,                                     -- Timestamp when the event actually occurred
    data JSONB  NOT NULL                         -- The event payload stored as JSON
);
